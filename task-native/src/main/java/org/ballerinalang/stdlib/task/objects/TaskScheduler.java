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
package org.ballerinalang.stdlib.task.objects;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.utils.TaskJob;
import org.ballerinalang.stdlib.task.utils.Utils;
import org.quartz.JobBuilder;
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

/**
 * Task scheduler handles the quartz scheduler functions.
 *
 * @since 2.0.0
 */
public class TaskScheduler {

    Scheduler scheduler = null;
    Map<String, JobKey> jobInfoMap;

    public TaskScheduler() throws SchedulingException {
        this.scheduler = TaskManager.getInstance().getScheduler();
        jobInfoMap = new HashMap<>();
    }

    public void stop(String triggerId) throws SchedulerException {
        Set<TriggerKey> keys = this.scheduler.getTriggerKeys(GroupMatcher.groupEquals(triggerId));
        if (!keys.isEmpty()) {
            for (TriggerKey key :keys) {
                this.scheduler.unscheduleJob(key);
            }

        }
    }

    public void start() throws SchedulerException {
        this.scheduler.start();
    }

    @SuppressWarnings("unchecked")
    public void addService(ServiceInformation serviceInformation, BMap<BString, Object> configurations,
                           String triggerId) throws SchedulerException {
        String name = serviceInformation.getServiceName();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TaskConstants.SERVICE_INFORMATION, serviceInformation);
        JobDetail job = JobBuilder.newJob(TaskJob.class).withIdentity(name).usingJobData(jobDataMap).build();
        Trigger trigger = Utils.getTrigger(configurations, triggerId);
        this.jobInfoMap.put(name, job.getKey());
        this.scheduler.scheduleJob(job, trigger);
    }

    @SuppressWarnings("unchecked")
    public void removeService(BObject service) throws SchedulerException {
        String serviceName = Utils.getServiceName(service);
        this.scheduler.deleteJob(this.jobInfoMap.get(serviceName));
        this.jobInfoMap.remove(serviceName);
    }

    public void pause(String triggerId) throws SchedulerException {
        this.scheduler.pauseTriggers(GroupMatcher.triggerGroupEquals(triggerId));
    }

    public void resume(String triggerId) throws SchedulerException {
        this.scheduler.resumeTriggers(GroupMatcher.triggerGroupEquals(triggerId));
    }
}
