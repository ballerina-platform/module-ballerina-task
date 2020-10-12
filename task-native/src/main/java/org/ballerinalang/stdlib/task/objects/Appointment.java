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

import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.utils.TaskJob;
import org.ballerinalang.stdlib.task.utils.Utils;
import org.quartz.*;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.spi.OperableTrigger;

import java.util.Calendar;
import java.util.Date;

/**
 * Represents an appointment.
 *
 * @since 0.995.0
 */
public class Appointment extends AbstractTask {

    private String cronExpression;
    private long thresholdInMillis, threadCount, threadPriority;
    private String policy;

    /**
     * Creates an Appointment object with provided CRON expression.
     *
     * @param configurations      Configurations related to a task.
     * @param threadConfiguration Configurations related to a ThreadPool.
     * @throws SchedulingException When initializing, this Appointment is failed.
     */
    public Appointment(BMap<BString, Object> configurations, BMap<BString, Object> threadConfiguration) throws
            SchedulingException {
        super();
        setConfigs(configurations, threadConfiguration);
    }

    /**
     * Creates an Appointment object with provided cron expression,
     * which will stop after running provided number of times.
     *
     * @param configurations      Configurations related to a task.
     * @param threadConfiguration Configurations related to a ThreadPool.
     * @param maxRuns        Number of times after which the Appointment will cancel.
     * @throws SchedulingException When initializing this Appointment is failed.
     */
    public Appointment(BMap<BString, Object> configurations, BMap<BString, Object> threadConfiguration, long maxRuns)
            throws SchedulingException {
        super(maxRuns);
        setConfigs(configurations, threadConfiguration);
    }

    /**
     * Returns the cron expression for this Appointment.
     *
     * @return cron expression for this appointment to trigger.
     */
    private String getCronExpression() {
        return this.cronExpression;
    }

    /**
     * Gets the number of maximum runs of this Appointment.
     *
     * @return the number of times the appointment runs before shutdown.
     */
    private long getMaxRuns() {
        return this.maxRuns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws SchedulingException {
        JobDataMap jobDataMap = getJobDataMapFromTask();
        try {
            scheduleAppointment(jobDataMap);
        } catch (SchedulerException e) {
            throw new SchedulingException("Failed to schedule Task.", e);
        }
    }

    /**
     * Schedule an Appointment.
     *
     * @param jobData     Map containing the details of the job.
     * @throws SchedulerException if scheduling is failed.
     */
    private void scheduleAppointment(JobDataMap jobData) throws SchedulerException, SchedulingException {
        String triggerId = this.getId();
        TaskManager.createSchedulerProperties(thresholdInMillis, threadCount, threadPriority);
        JobDetail job = JobBuilder.newJob(TaskJob.class).usingJobData(jobData).withIdentity(triggerId).build();
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerId)
                .withSchedule(buildCronScheduler(this.getCronExpression()))
                .build();

        if (this.getMaxRuns() > 0) {
            int repeatCount = (int) (this.getMaxRuns() - 1);
            Date endDate = TriggerUtils.computeEndTimeToAllowParticularNumberOfFirings((OperableTrigger) trigger,
                    new BaseCalendar(Calendar.getInstance().getTimeZone()), repeatCount);

            trigger = trigger.getTriggerBuilder().endAt(endDate).build();

        }
        TaskManager.getInstance().getScheduler().scheduleJob(job, trigger);
        quartzJobs.put(triggerId, job.getKey());
    }

    private CronScheduleBuilder buildCronScheduler(String cronExpression) {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
        switch (policy) {
            case TaskConstants.DO_NOTHING:
                cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
                break;
            case TaskConstants.IGNORE_POLICY:
                cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                break;
            case TaskConstants.FIRE_AND_PROCEED:
                cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
                break;
            default:
        }
        return cronScheduleBuilder;
    }

    public void setConfigs(BMap<BString, Object> configurations, BMap<BString, Object> threadConfiguration) throws
            SchedulingException {
        Object appointmentDetails = configurations.get(TaskConstants.MEMBER_APPOINTMENT_DETAILS);
        this.cronExpression = Utils.getCronExpressionFromAppointmentRecord(appointmentDetails);
        this.thresholdInMillis = configurations.getIntValue(TaskConstants.THRESHOLD_IN_MILLIS).intValue();
        this.policy = String.valueOf(configurations.getStringValue(TaskConstants.MISFIRE_POLICY));
        this.threadCount = threadConfiguration.getIntValue(TaskConstants.THREAD_COUNT).intValue();
        this.threadPriority = threadConfiguration.getIntValue(TaskConstants.THREAD_PRIORITY).intValue();
    }
}
