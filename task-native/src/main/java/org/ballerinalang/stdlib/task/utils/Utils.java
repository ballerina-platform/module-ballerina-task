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
package org.ballerinalang.stdlib.task.utils;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.AttachedFunctionType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerUtils;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.spi.OperableTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/**
 * Utility functions used in ballerina task module.
 *
 * @since 0.995.0
 */
public class Utils {

    // valid resource count is set to to because the init function is also received as
    // an attached function.
    private static final int VALID_RESOURCE_COUNT = 1;

    public static BError createListenerError(String message) {
        return createTaskError(TaskConstants.LISTENER_ERROR, message);
    }

    public static BError createSchedulerError(String message) {
        return createTaskError(TaskConstants.SCHEDULER_ERROR, message);
    }

    public static BError createTaskError(String reason, String message) {
        return ErrorCreator.createDistinctError(reason, TaskConstants.TASK_PACKAGE_ID,
                StringUtils.fromString(message));
    }

    @SuppressWarnings("unchecked")
    public static String validateCronExpression(BString expression) throws SchedulingException {
        String cronExpression = String.valueOf(expression);
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new SchedulingException("Cron Expression \"" + cronExpression + "\" is invalid.");
        }
        return cronExpression;
    }

    /*
     * TODO: Runtime validation is done as compiler plugin does not work right now.
     *       When compiler plugins can be run for the resources without parameters, this will be redundant.
     *       Issue: https://github.com/ballerina-platform/ballerina-lang/issues/14148
     */
    public static void validateService(BObject service) throws SchedulingException {
        AttachedFunctionType[] resources = service.getType().getAttachedFunctions();
        if (resources.length != VALID_RESOURCE_COUNT) {
            throw new SchedulingException("Invalid number of resources found in service \'" +
                    Utils.getServiceName(service) + "\'. Task service should include only one resource.");
        }
        AttachedFunctionType resource = resources[0];

        if (TaskConstants.RESOURCE_ON_TRIGGER.equals(resource.getName())) {
            validateOnTriggerResource(resource.getReturnParameterType());
        } else {
            throw new SchedulingException("Invalid resource function found: " + resource.getName()
                    + ". Expected: \'" + TaskConstants.RESOURCE_ON_TRIGGER + "\'.");
        }
    }

    private static void validateOnTriggerResource(Type returnParameterType) throws SchedulingException {
        if (returnParameterType != io.ballerina.runtime.api.PredefinedTypes.TYPE_NULL) {
            throw new SchedulingException("Invalid resource function signature: \'" +
                    TaskConstants.RESOURCE_ON_TRIGGER + "\' should not return a value.");
        }
    }

    public static String getServiceName(BObject service) {
        return service.getType().getName().split("\\$\\$")[0];
    }

    public static Trigger createTrigger(SimpleScheduleBuilder scheduleBuilder, long delay, String triggerId) {
        Trigger trigger;
        String triggerKey = UUID.randomUUID().toString();
        if (delay > 0) {
            Date startTime = new Date(System.currentTimeMillis() + delay);
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey, triggerId)
                    .startAt(startTime)
                    .withSchedule(scheduleBuilder)
                    .build();
        } else {
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey, triggerId)
                    .startNow()
                    .withSchedule(scheduleBuilder)
                    .build();
        }
        return trigger;
    }

    public static SimpleScheduleBuilder createSchedulerBuilder(BMap<BString, Object> configurations) {
        long interval = configurations.getIntValue(TaskConstants.FIELD_INTERVAL).intValue();
        long maxRuns = configurations.getIntValue(TaskConstants.FIELD_NO_OF_RUNS).intValue();;
        String policy = String.valueOf(configurations.getStringValue(TaskConstants.MISFIRE_POLICY));
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(interval);
        if (maxRuns > 0) {
            // Quartz uses the number of repeats but the total number of runs is counted here.
            // Hence, `maxRuns` is subtracted by 1 to get the repeat count.
            simpleScheduleBuilder.withRepeatCount((int) (maxRuns - 1));
            if (maxRuns == 1) {
                if (policy.equals(TaskConstants.FIRE_NOW)) {
                    simpleScheduleBuilder.withMisfireHandlingInstructionFireNow();
                } else if (policy.equals(TaskConstants.IGNORE_POLICY)) {
                    simpleScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                }
            } else {
                setMisfirePolicyForRecurringAction(simpleScheduleBuilder, policy);
            }
        } else {
            simpleScheduleBuilder.repeatForever();
            setMisfirePolicyForRecurringAction(simpleScheduleBuilder, policy);
        }
        return simpleScheduleBuilder;
    }

    private static void setMisfirePolicyForRecurringAction(SimpleScheduleBuilder simpleScheduleBuilder, String policy) {
        switch (policy) {
            case TaskConstants.NEXT_WITH_EXISTING_COUNT:
                simpleScheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
                break;
            case TaskConstants.IGNORE_POLICY:
                simpleScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                break;
            case TaskConstants.NEXT_WITH_REMAINING_COUNT:
                simpleScheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
                break;
            case TaskConstants.NOW_WITH_EXISTING_COUNT:
                simpleScheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
                break;
            case TaskConstants.NOW_WITH_REMAINING_COUNT:
                simpleScheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
                break;
            default:
                break;
        }
    }

    public static CronTrigger createCronTrigger(CronScheduleBuilder scheduleBuilder, long maxRuns,
                                                String triggerID) {
        String triggerKey = UUID.randomUUID().toString();
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey, triggerID)
                .withSchedule(scheduleBuilder)
                .build();
        if (maxRuns > 0) {
            int repeatCount = (int) (maxRuns - 1);
            Date endDate = TriggerUtils.computeEndTimeToAllowParticularNumberOfFirings((OperableTrigger) trigger,
                    new BaseCalendar(Calendar.getInstance().getTimeZone()), repeatCount);

            trigger = trigger.getTriggerBuilder().endAt(endDate).build();
        }
        return trigger;
    }

    public static CronScheduleBuilder createCronScheduleBuilder(String cronExpression, String policy) {
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

    public static Trigger getTrigger(BMap<BString, Object> configurations, String triggerID) {
        Trigger trigger;
        String configurationTypeName = configurations.getType().getName();
        if (TaskConstants.SIMPLE_TRIGGER_CONFIGURATION.equals(configurationTypeName)) {
            long delay = configurations.getIntValue(TaskConstants.FIELD_DELAY).intValue();
            SimpleScheduleBuilder scheduleBuilder = Utils.createSchedulerBuilder(configurations);
            trigger = Utils.createTrigger(scheduleBuilder, delay, triggerID);
        } else {
            Object cronExpression = configurations.get(TaskConstants.MEMBER_CRON_EXPRESSION);
            String policy = String.valueOf(configurations.getStringValue(TaskConstants.MISFIRE_POLICY));
            CronScheduleBuilder scheduleBuilder = Utils.createCronScheduleBuilder(cronExpression.toString(),
                    policy);
            long maxRuns = configurations.getIntValue(TaskConstants.FIELD_NO_OF_RUNS).intValue();;
            trigger = Utils.createCronTrigger(scheduleBuilder, maxRuns, triggerID);
        }
        return trigger;
    }

    public static ServiceInformation getServiceInformation(BObject service, Runtime runtime,
                                                           Object... attachments) throws SchedulingException {
        Utils.validateService(service);
        ServiceInformation serviceInformation;
        if (attachments == null) {
            serviceInformation = new ServiceInformation(runtime, service);
        } else {
            serviceInformation = new ServiceInformation(runtime, service, attachments);
        }
        return serviceInformation;
    }

    public static JobDetail createJob(JobDataMap jobDataMap, String jobId) {
        return JobBuilder.newJob(TaskJob.class).withIdentity(jobId).usingJobData(jobDataMap).build();
    }

    public static Properties createSchedulerProperties() {
        Properties properties = new Properties();
        properties.setProperty(TaskConstants.QUARTZ_MISFIRE_THRESHOLD, TaskConstants.QUARTZ_THRESHOLD_VALUE);
        properties.setProperty(TaskConstants.QUARTZ_THREAD_COUNT, TaskConstants.QUARTZ_THREAD_COUNT_VALUE);
        return properties;
    }
}
