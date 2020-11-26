/*
 *  Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.stdlib.task.objects;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.utils.Utils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Task scheduler handles the quartz scheduler functions.
 *
 * @since 2.0.0
 */
public class TaskScheduler {

    Scheduler scheduler = null;
    Map<String, JobKey> jobInfoMap;
    Map<String, TriggerKey> triggerInfoMap;

    public TaskScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        jobInfoMap = new HashMap<>();
        triggerInfoMap = new HashMap<>();
    }

    public void stop(String triggerId) throws SchedulerException {
        Set<TriggerKey> keys = this.scheduler.getTriggerKeys(GroupMatcher.groupEquals(triggerId));
        if (!keys.isEmpty()) {
            for (TriggerKey key :keys) {
                this.scheduler.unscheduleJob(key);
            }
        }
    }

    public void stop() throws SchedulerException {
        this.scheduler.shutdown();
    }

    public void gracefulStop() throws SchedulerException {
        this.scheduler.shutdown(true);
    }

    public void start() throws SchedulerException {
        this.scheduler.start();
    }

    public void addService(ServiceInformation serviceInformation, BMap<BString, Object> configurations,
                           String triggerId) throws SchedulerException {
        String name = serviceInformation.getServiceName();
        String jobId = String.valueOf(UUID.randomUUID().hashCode());
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TaskConstants.JOB, serviceInformation);
        JobDetail job = Utils.createJob(jobDataMap, jobId);
        this.jobInfoMap.put(name, job.getKey());
        Trigger trigger = Utils.getTrigger(configurations, triggerId);
        this.scheduler.scheduleJob(job, trigger);
    }

    public void scheduleJob(JobDataMap jobDataMap, BMap<BString, Object> configurations, String jobId)
            throws SchedulerException {
        JobDetail job = Utils.createJob(jobDataMap, jobId);
        Trigger trigger = Utils.getTrigger(configurations, "trigger");
        this.jobInfoMap.put(jobId, job.getKey());
        this.scheduler.scheduleJob(job, trigger);
        this.triggerInfoMap.put(jobId, trigger.getKey());
        if (!this.scheduler.isStarted()) {
            this.scheduler.start();
        }
    }

    public void removeService(BObject service) throws SchedulerException {
        String serviceName = Utils.getServiceName(service);
        this.scheduler.deleteJob(this.jobInfoMap.get(serviceName));
        this.jobInfoMap.remove(serviceName);
        this.triggerInfoMap.remove(serviceName);
    }

    public void pause(String triggerId) throws SchedulerException {
        this.scheduler.pauseTriggers(GroupMatcher.triggerGroupEquals(triggerId));
    }

    public void resume(String triggerId) throws SchedulerException {
        this.scheduler.resumeTriggers(GroupMatcher.triggerGroupEquals(triggerId));
    }

    public void pause() throws SchedulerException {
        this.scheduler.pauseAll();
    }

    public void resume() throws SchedulerException {
        this.scheduler.resumeAll();
    }

    public boolean isRunning() throws SchedulerException {
        return this.scheduler.isStarted();
    }

    public void pauseJob(String jobId) throws SchedulerException {
        this.scheduler.pauseJob(this.jobInfoMap.get(jobId));
    }

    public void resumeJob(String jobId) throws SchedulerException {
        this.scheduler.resumeJob(this.jobInfoMap.get(jobId));
    }

    public void unScheduleJob(String jobId) throws SchedulerException {
        this.scheduler.unscheduleJob(this.triggerInfoMap.get(jobId));
    }

    public void rescheduleJob(String jobId, Trigger trigger) throws SchedulerException {
        this.scheduler.rescheduleJob(this.triggerInfoMap.get(jobId), trigger);
        this.triggerInfoMap.put(jobId, trigger.getKey());
    }

    public void deleteJob(String jobId) throws SchedulerException {
        this.scheduler.deleteJob(this.jobInfoMap.get(jobId));
        jobInfoMap.remove(jobId);
        triggerInfoMap.remove(jobId);
    }

    public Set<String> getAllRunningJobs() {
        for (Map.Entry<String, TriggerKey> entry : triggerInfoMap.entrySet()) {
            try {
                String jobId = entry.getKey();
                Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerInfoMap.get(jobId));
                if (triggerState.equals(Trigger.TriggerState.COMPLETE) ||
                        triggerState.equals(Trigger.TriggerState.NONE)) {
                    jobInfoMap.remove(jobId);
                }
            } catch (SchedulerException e) {
                //
            }
        }
        return jobInfoMap.keySet();
    }
}
