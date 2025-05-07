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
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.exceptions.SchedulingException;
import io.ballerina.stdlib.task.objects.TaskManager;
import io.ballerina.stdlib.task.utils.TaskConstants;
import io.ballerina.stdlib.task.utils.Utils;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskListener {
    private static final int bound = 1000000;
    private static final String value = "1000";
    private String type;
    private final Map<String, BObject> serviceRegistry = new ConcurrentHashMap<>();
    private BMap<BString, Object> configs = ValueCreator.createMapValue();
    private Boolean isTaskCoordinator = false;

    public TaskListener() {

    }

    public void start(Environment env, BObject job, long time) {
        Utils.disableQuartzLogs();
        try {
            getScheduler(env);
            for (String serviceName : serviceRegistry.keySet()) {
                Integer jobId = java.security.SecureRandom.getInstanceStrong().nextInt(bound);
                JobDataMap jobDataMap = getJobDataMap(job, TaskConstants.LOG_AND_CONTINUE, String.valueOf(jobId));
                BObject service = serviceRegistry.get(serviceName);
                TaskManager.getInstance().scheduleOneTimeListenerJob(jobDataMap, time, jobId, service);
            }
        } catch (SchedulerException | SchedulingException | IllegalArgumentException | NoSuchAlgorithmException e) {
        }
    }

    public void start(Environment env, BObject job, BDecimal interval, long maxCount,
                      Object startTime, Object endTime, BMap<BString, Object> policy) {
        try {
            getScheduler(env);
            for (String serviceName : serviceRegistry.keySet()) {
                int jobId = java.security.SecureRandom.getInstanceStrong().nextInt(bound);
                JobDataMap jobDataMap = getJobDataMap(job, ((BString) policy.get(TaskConstants.ERR_POLICY)).getValue(),
                        String.valueOf(jobId));
                BObject service = serviceRegistry.get(serviceName);
                TaskManager.getInstance().scheduleListenerIntervalJob(jobDataMap,
                        (interval.decimalValue().multiply(new BigDecimal(value))).longValue(), maxCount, startTime,
                        endTime, ((BString) policy.get(TaskConstants.WAITING_POLICY)).getValue(), jobId, service);
            }

        } catch (Exception e) {
        }
    }

    public void setConfig(BString key, Object value) {
        configs.put(key, value);
    }

    public void setConfigs(BMap<?, ?> values) {
        configs.merge(values, false);
    }

    public BMap<BString, Object> getConfig() {
        return configs;
    }

    public String getType() {
        return type;
    }

    public Boolean isTaskCoordinator() {
        return isTaskCoordinator;
    }

    public void setTaskCoordinator(Boolean isTaskCoordinator) {
        this.isTaskCoordinator = isTaskCoordinator;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void registerService(String serviceName, BObject service) {
        serviceRegistry.put(serviceName, service);
    }

    public void unregisterService(String serviceName) {
        serviceRegistry.remove(serviceName);
    }

    public BObject getService(String serviceName) {
        return serviceRegistry.get(serviceName);
    }

    private static Scheduler getScheduler(Environment env) throws SchedulingException, SchedulerException {
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
