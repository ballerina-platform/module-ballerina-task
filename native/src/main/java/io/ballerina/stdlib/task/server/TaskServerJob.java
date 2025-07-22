/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.task.server;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.coordination.DatabaseConfig;
import io.ballerina.stdlib.task.coordination.TokenAcquisition;
import io.ballerina.stdlib.task.objects.TaskManager;
import io.ballerina.stdlib.task.utils.TaskConstants;
import io.ballerina.stdlib.task.utils.Utils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import static io.ballerina.stdlib.task.coordination.TokenAcquisition.attemptTokenAcquisition;
import static io.ballerina.stdlib.task.coordination.TokenAcquisition.hasActiveToken;
import static io.ballerina.stdlib.task.objects.TaskManager.DATABASE_CONFIG;
import static io.ballerina.stdlib.task.objects.TaskManager.INTERVAL;
import static io.ballerina.stdlib.task.objects.TaskManager.LIVENESS_CHECK_INTERVAL;
import static io.ballerina.stdlib.task.objects.TaskManager.MAX_COUNT;
import static io.ballerina.stdlib.task.objects.TaskManager.TASK_ID;
import static io.ballerina.stdlib.task.objects.TaskManager.TOKEN_HOLDER;

public class TaskServerJob implements Job {
    public static final String GROUP_ID = "groupId";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Thread.startVirtualThread(() -> {
            Runtime runtime = TaskManager.getInstance().getRuntime();
            Boolean isTokenHolder = (Boolean) jobExecutionContext.getMergedJobDataMap().get(TOKEN_HOLDER);
            BObject job = (BObject) jobExecutionContext.getMergedJobDataMap().get(TaskConstants.JOB);
            if (isTokenHolder == null) {
                executeJob(job, runtime, jobExecutionContext);
                return;
            }
            DatabaseConfig dbConfig = (DatabaseConfig) jobExecutionContext.getMergedJobDataMap().get(DATABASE_CONFIG);
            String taskId = ((BString) jobExecutionContext.getMergedJobDataMap().get(TASK_ID)).getValue();
            String groupId = ((BString) jobExecutionContext.getMergedJobDataMap().get(GROUP_ID)).getValue();
            String jdbcUrl = TokenAcquisition.getJdbcUrl(dbConfig);
            processJobWithCoordination(job, runtime, jobExecutionContext, isTokenHolder,
                    taskId, groupId, jdbcUrl, dbConfig);
        });
    }

    private void  processJobWithCoordination(BObject job, Runtime runtime, JobExecutionContext jobExecutionContext,
                                             boolean isTokenHolder, String taskId, String groupId,
                                             String jdbcUrl, DatabaseConfig dbConfig) {
        Connection connection = null;
        boolean deadStatus = false;
        try {
            connection = DriverManager.getConnection(jdbcUrl, dbConfig.user(), dbConfig.password());
        } catch (Exception e) {
            deadStatus = true;
        }
        try {
            if (!deadStatus) {
                connection.setAutoCommit(false);
                boolean shouldExecuteJob = checkAndUpdateTokenStatus(connection, jobExecutionContext, taskId,
                        groupId, isTokenHolder, dbConfig);
                connection.commit();
                if (shouldExecuteJob) {
                    executeJob(job, runtime, jobExecutionContext);
                }
            }
        } catch (SQLException e) {
            handleExecutionException(connection, jobExecutionContext,
                    ErrorCreator.createError(StringUtils.fromString("Database error: " + e.getMessage())));
        } catch (BError error) {
            handleExecutionException(connection, jobExecutionContext, error);
        } catch (Throwable t) {
            handleExecutionException(connection, jobExecutionContext, ErrorCreator.createError(t));
        }
    }

    private boolean checkAndUpdateTokenStatus(Connection connection, JobExecutionContext jobExecutionContext,
                                              String taskId, String groupId, boolean isTokenHolder,
                                              DatabaseConfig dbConfig)
            throws SQLException {
        if (isTokenHolder) {
            return hasActiveToken(connection, taskId, groupId);
        }
        int livenessInterval = (int) jobExecutionContext.getMergedJobDataMap().get(LIVENESS_CHECK_INTERVAL);
        return attemptTokenAcquisition(connection, taskId, groupId, false,
                livenessInterval, dbConfig.dbType());
    }

    private void handleExecutionException(Connection connection,
                                          JobExecutionContext jobExecutionContext, BError error) {
        try {
            handleRollback(connection);
            Utils.notifyFailure(jobExecutionContext, error);
        } catch (SQLException e) {
            Utils.notifyFailure(jobExecutionContext, error);
        }
    }

    private void executeJob(BObject job, Runtime runtime, JobExecutionContext jobExecutionContext) {
        ObjectType type = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(job));
        boolean isConcurrentSafe = type.isIsolated() && type.isIsolated(TaskConstants.EXECUTE);
        StrandMetadata metadata = new StrandMetadata(isConcurrentSafe, null);
        Object result = runtime.callMethod(job, TaskConstants.EXECUTE, metadata);
        if (result instanceof BError) {
            JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
            if (shouldRetry(jobDataMap)) {
                Long maxCount = (Long) jobDataMap.get(MAX_COUNT);
                jobExecutionContext.getMergedJobDataMap().put(MAX_COUNT, maxCount - 1);
                result = executeWithRetry(job, runtime, jobDataMap, metadata);
            }
            if (result instanceof BError) {
                Utils.notifyFailure(jobExecutionContext, (BError) result);
            }
        }
    }

    private Object executeWithRetry(BObject job, Runtime runtime,
                                    Map<String, Object> jobDataMap, StrandMetadata metadata) {
        Long maxAttempts = (Long) jobDataMap.get("maxAttempts");
        String backoffStrategy = jobDataMap.get("backoffStrategy").toString();
        Long retryInterval = (Long) jobDataMap.get("retryInterval");
        Long maxInterval = (Long) jobDataMap.get("maxInterval");
        Object result = null;
        long currentInterval = retryInterval;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            result = runtime.callMethod(job, TaskConstants.EXECUTE, metadata);
            if (!(result instanceof BError)) {
                break;
            }
            if (attempt < maxAttempts) {
                setTimeout(currentInterval);
                currentInterval = calculateNextInterval(backoffStrategy, currentInterval, retryInterval, maxInterval);
                BDecimal taskInterval = (BDecimal) jobDataMap.get("interval");
                if (currentInterval > taskInterval.intValue()) {
                    break;
                }
            }
        }
        return result;
    }

    private long calculateNextInterval(String backoffStrategy,
                                       long currentInterval, long retryInterval, long maxInterval) {
        return switch (backoffStrategy) {
            case "EXPONENTIAL" -> Math.min(currentInterval * 2, maxInterval);
            case "LINEAR" -> currentInterval + retryInterval;
            default -> currentInterval;
        };
    }

    private boolean shouldRetry(Map<String, Object> jobDataMap) {
        Long maxCount = (Long) jobDataMap.get(MAX_COUNT);
        if (maxCount == null || maxCount == 1) {
            return false;
        }
        BDecimal taskInterval = (BDecimal) jobDataMap.get(INTERVAL);
        if (!jobDataMap.containsKey("retryInterval")) {
            return false;
        }
        Long retryInterval = (Long) jobDataMap.get("retryInterval");
        if (retryInterval > taskInterval.intValue()) {
            return false;
        }
        Long maxAttempts = (Long) jobDataMap.get("maxAttempts");
        return maxAttempts != null && maxAttempts > 0;
    }

    public static void setTimeout(long retryInterval) {
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public static void handleRollback(Connection connection) throws SQLException {
        if (connection != null) {
            connection.rollback();
        }
    }
}
