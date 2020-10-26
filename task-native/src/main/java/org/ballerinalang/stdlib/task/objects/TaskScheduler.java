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
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Task scheduler handles the quartz scheduler functions.
 */
public class TaskScheduler {

    Scheduler scheduler = null;
    Map<String, JobKey> jobInfoMap;

    public TaskScheduler(Properties properties) throws SchedulerException {
        StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory(properties);
        this.scheduler = stdSchedulerFactory.getScheduler();
        jobInfoMap = new HashMap<>();
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

    @SuppressWarnings("unchecked")
    public void addService(BObject taskListener, ServiceInformation serviceInformation)
            throws SchedulerException {
        BMap<BString, Object> configurations = taskListener.getMapValue(TaskConstants.MEMBER_LISTENER_CONFIGURATION);
        String name = serviceInformation.getServiceName();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TaskConstants.SERVICE_INFORMATION, serviceInformation);
        JobDetail job = JobBuilder.newJob(TaskJob.class).withIdentity(name).usingJobData(jobDataMap).build();
        this.jobInfoMap.put(name, job.getKey());
        Trigger trigger;
        String configurationTypeName = configurations.getType().getName();
        try {
            if (TaskConstants.RECORD_TIMER_CONFIGURATION.equals(configurationTypeName)) {
                long delay = configurations.getIntValue(TaskConstants.FIELD_DELAY).intValue();
                SimpleScheduleBuilder scheduleBuilder = Utils.createSchedulerBuilder(configurations);
                trigger = Utils.createTrigger(scheduleBuilder, name, delay);
            } else {
                Object appointmentDetails = configurations.get(TaskConstants.MEMBER_CRON_EXPRESSION);
                String cronExpression = Utils.getCronExpressionFromAppointmentRecord(appointmentDetails);
                String policy = String.valueOf(configurations.getStringValue(TaskConstants.MISFIRE_POLICY));
                CronScheduleBuilder scheduleBuilder = Utils.createCronScheduleBuilder(cronExpression, policy);
                long maxRuns = Utils.getMaxRuns(configurations);
                trigger = Utils.createCronTrigger(scheduleBuilder, name, maxRuns);
            }
        } catch (SchedulingException e) {
            throw Utils.createTaskError(e.getMessage());
        }
        this.scheduler.scheduleJob(job, trigger);
    }

    @SuppressWarnings("unchecked")
    public void removeService(BObject service) throws SchedulerException {
        String serviceName = Utils.getServiceName(service);
        this.scheduler.deleteJob(this.jobInfoMap.get(serviceName));
        this.jobInfoMap.remove(serviceName);
    }

    public void pause() throws SchedulerException {
        this.scheduler.pauseAll();
    }

    public void resume() throws SchedulerException {
        this.scheduler.resumeAll();
    }
}
