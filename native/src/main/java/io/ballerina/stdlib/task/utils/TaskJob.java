/*
 *  Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.task.utils;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.DatabaseConfig;
import io.ballerina.stdlib.task.objects.TaskManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static io.ballerina.stdlib.task.TokenAcquisition.JDBC_URL;
import static io.ballerina.stdlib.task.TokenAcquisition.attemptTokenAcquisition;
import static io.ballerina.stdlib.task.TokenAcquisition.hasActiveToken;
import static io.ballerina.stdlib.task.TokenAcquisition.upsertToken;
import static io.ballerina.stdlib.task.objects.TaskManager.DATABASE_CONFIG;
import static io.ballerina.stdlib.task.objects.TaskManager.INSTANCE_ID;
import static io.ballerina.stdlib.task.objects.TaskManager.LIVENESS_INTERVAL;
import static io.ballerina.stdlib.task.objects.TaskManager.TOKEN_HOLDER;
import static io.ballerina.stdlib.task.utils.Utils.rollbackIfNeeded;

/**
 * Represents a Quartz job related to an appointment.
 */
public class TaskJob implements Job {

    public TaskJob() {
    }

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
            String instanceId = ((BString) jobExecutionContext.getMergedJobDataMap().get(INSTANCE_ID)).getValue();
            String jdbcUrl = String.format(JDBC_URL, dbConfig.host(), dbConfig.port(), dbConfig.database());
            processJobWithCoordination(job, runtime, jobExecutionContext, isTokenHolder, instanceId, jdbcUrl, dbConfig);
        });
    }

    private void processJobWithCoordination(BObject job, Runtime runtime, JobExecutionContext jobExecutionContext,
                                            boolean isTokenHolder, String instanceId,
                                            String jdbcUrl, DatabaseConfig dbConfig) {
        Connection connection = null;
        boolean needsRollback = false;

        try {
            connection = DriverManager.getConnection(jdbcUrl, dbConfig.user(), dbConfig.password());
            connection.setAutoCommit(false);
            needsRollback = true;

            boolean shouldExecuteJob = checkAndUpdateTokenStatus(connection, jobExecutionContext,
                                                                 instanceId, isTokenHolder);
            connection.commit();
            needsRollback = false;

            if (shouldExecuteJob) {
                executeJob(job, runtime, jobExecutionContext);
            }
        } catch (SQLException e) {
            handleExecutionException(connection, needsRollback, jobExecutionContext,
                    ErrorCreator.createError(StringUtils.fromString("Database error: " + e.getMessage())));
        } catch (BError error) {
            handleExecutionException(connection, needsRollback, jobExecutionContext, error);
        } catch (Throwable t) {
            handleExecutionException(connection, needsRollback, jobExecutionContext, ErrorCreator.createError(t));
        }
    }

    private boolean checkAndUpdateTokenStatus(Connection connection, JobExecutionContext jobExecutionContext,
                                              String instanceId, boolean isTokenHolder) throws SQLException {
        if (isTokenHolder) {
            return hasActiveToken(connection, instanceId);
        }
        int livenessInterval = (int) jobExecutionContext.getMergedJobDataMap().get(LIVENESS_INTERVAL);
        boolean acquiredToken = attemptTokenAcquisition(connection, instanceId, false, livenessInterval);
        if (!acquiredToken) {
            upsertToken(connection, instanceId);
        }
        return acquiredToken;
    }

    private void handleExecutionException(Connection connection, boolean needsRollback,
                                          JobExecutionContext jobExecutionContext, BError error) {
        rollbackIfNeeded(connection, needsRollback);
        Utils.notifyFailure(jobExecutionContext, error);
    }

    private void executeJob(BObject job, Runtime runtime, JobExecutionContext jobExecutionContext) {
        try {
            ObjectType objectType = (ObjectType) job.getOriginalType();
            boolean isConcurrentSafe = objectType.isIsolated() && objectType.isIsolated(TaskConstants.EXECUTE);
            StrandMetadata metadata = new StrandMetadata(isConcurrentSafe, null);
            runtime.callMethod(job, TaskConstants.EXECUTE, metadata);
        } catch (BError error) {
            Utils.notifyFailure(jobExecutionContext, error);
        } catch (Throwable t) {
            Utils.notifyFailure(jobExecutionContext, ErrorCreator.createError(t));
        }
    }
}
