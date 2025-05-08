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

package io.ballerina.stdlib.task.listener;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.coordination.TokenAcquisition;
import io.ballerina.stdlib.task.exceptions.SchedulingException;
import io.ballerina.stdlib.task.objects.TaskManager;
import io.ballerina.stdlib.task.utils.TaskConstants;
import io.ballerina.stdlib.task.utils.Utils;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskListener {
    private static final String VALUE = "1000";
    private final TaskManager taskManager;
    public static final BString DATABASE_CONFIG = StringUtils.fromString("databaseConfig");
    public static final BString TASK_ID = StringUtils.fromString("taskId");
    public static final BString GROUP_ID = StringUtils.fromString("groupId");
    public static final BString LIVENESS_CHECK_INTERVAL = StringUtils.fromString("livenessCheckInterval");
    public static final BString HEARTBEAT_FREQUENCY = StringUtils.fromString("heartbeatFrequency");
    private final Map<String, BObject> serviceRegistry = new ConcurrentHashMap<>();
    private final BMap<BString, Object> configs = ValueCreator.createMapValue();

    public TaskListener(TaskManager taskManager, String type) {
        this.taskManager = taskManager;
        this.type = type;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public TaskListener(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public void start(Environment env, BObject job, BDecimal interval, long maxCount,
                      Object startTime, Object endTime, BMap<BString, Object> policy) throws Exception {
        getScheduler(env);
        for (String serviceName : serviceRegistry.keySet()) {
            JobDataMap jobDataMap =
                    getJobDataMap(job, ((BString) policy.get(TaskConstants.ERR_POLICY)).getValue(), serviceName);
            BObject service = serviceRegistry.get(serviceName);
            this.taskManager.scheduleListenerIntervalJob(jobDataMap,
                    (interval.decimalValue().multiply(new BigDecimal(VALUE))).longValue(), maxCount, startTime,
                    endTime, ((BString) policy.get(TaskConstants.WAITING_POLICY)).getValue(), serviceName, service);
        }
    }

    public void start(Environment env, BObject job, BDecimal interval, long maxCount,
                      Object startTime, Object endTime, BMap<BString, Object> policy,
                      BMap warmBackupConfig) throws Exception {
        getScheduler(env);
        for (String serviceName : serviceRegistry.keySet()) {
            int jobId = java.security.SecureRandom.getInstanceStrong().nextInt(bound);
            JobDataMap jobDataMap = getJobDataMap(job, ((BString) policy.get(TaskConstants.ERR_POLICY)).getValue(),
                    String.valueOf(jobId));
            BObject service = serviceRegistry.get(serviceName);
            BMap<Object, Object> databaseConfig = warmBackupConfig.getMapValue(DATABASE_CONFIG);
            BString id = warmBackupConfig.getStringValue(TASK_ID);
            BString groupId = warmBackupConfig.getStringValue(GROUP_ID);
            int livenessInterval = ((Long) warmBackupConfig.get(LIVENESS_CHECK_INTERVAL)).intValue();
            int heartbeatFrequency = ((Long) warmBackupConfig.get(HEARTBEAT_FREQUENCY)).intValue();
            BMap response = (BMap) TokenAcquisition.acquireToken(databaseConfig, id, groupId, false,
                    livenessInterval, heartbeatFrequency);
            this.taskManager.scheduleListenerIntervalJobWithTokenCheck(jobDataMap,
                    (interval.decimalValue().multiply(new BigDecimal(value))).longValue(), maxCount, startTime,
                    endTime, ((BString) policy.get(TaskConstants.WAITING_POLICY)).getValue(),
                    jobId, response, service);
        }
    }

    public Map<String, BObject> getServices() {
        return serviceRegistry;
    }

    public void setConfigs(BMap<?, ?> values) {
        configs.merge(values, false);
    }

    public BMap<BString, Object> getConfig() {
        return configs;
    }

    public void registerService(String serviceName, BObject service) {
        serviceRegistry.put(serviceName, service);
    }

    public void unregisterService(Object service) throws Exception {
        String serviceId = null;
        for (Map.Entry<String, BObject> entry : serviceRegistry.entrySet()) {
            if (entry.getValue().equals(service)) {
                serviceId = entry.getKey();
                break;
            }
        }
        if (serviceId != null) {
            serviceRegistry.remove(serviceId);
            taskManager.unScheduleJob(serviceId);
        } else {
            throw new Exception("Service is not found in the listener");
        }
    }

    public void unregisterAllServices() {
        serviceRegistry.clear();
    }

    static Scheduler getScheduler(Environment env) throws SchedulingException, SchedulerException {
        Utils.disableQuartzLogs();
        return TaskManager.getInstance().getScheduler(Utils.createSchedulerProperties(
                TaskConstants.QUARTZ_THREAD_COUNT_VALUE, TaskConstants.QUARTZ_THRESHOLD_VALUE), env);
    }

    private static JobDataMap getJobDataMap(BObject job, String errorPolicy, String jobId) {
        Utils.disableQuartzLogs();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TaskConstants.JOB, job);
        jobDataMap.put(TaskConstants.ERROR_POLICY, errorPolicy);
        jobDataMap.put(TaskConstants.JOB_ID, jobId);
        return jobDataMap;
    }
}
