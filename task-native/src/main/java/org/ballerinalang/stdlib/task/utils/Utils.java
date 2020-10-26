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

import io.ballerina.runtime.TypeChecker;
import io.ballerina.runtime.api.ErrorCreator;
import io.ballerina.runtime.api.StringUtils;
import io.ballerina.runtime.api.types.AttachedFunctionType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerUtils;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.spi.OperableTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import static org.ballerinalang.stdlib.task.utils.TaskConstants.QUARTZ_THREAD_COUNT_VALUE;

/**
 * Utility functions used in ballerina task module.
 *
 * @since 0.995.0
 */
public class Utils {

    // valid resource count is set to to because the init function is also received as
    // an attached function.
    private static final int VALID_RESOURCE_COUNT = 1;

    public static BError createTaskError(String message) {
        return createTaskError(TaskConstants.LISTENER_ERROR, message);
    }

    public static BError createTaskError(String reason, String message) {
        return ErrorCreator.createDistinctError(reason, TaskConstants.TASK_PACKAGE_ID,
                StringUtils.fromString(message));
    }

    @SuppressWarnings("unchecked")
    public static String getCronExpressionFromAppointmentRecord(Object record) throws SchedulingException {
        String cronExpression;
        if (TaskConstants.RECORD_APPOINTMENT_DATA.equals(TypeChecker.getType(record).getName())) {
            cronExpression = buildCronExpression((BMap<BString, Object>) record);
            if (!CronExpression.isValidExpression(cronExpression)) {
                throw new SchedulingException("AppointmentData \"" + record.toString() + "\" is invalid.");
            }
        } else {
            cronExpression = record.toString();
            if (!CronExpression.isValidExpression(cronExpression)) {
                throw new SchedulingException("Cron Expression \"" + cronExpression + "\" is invalid.");
            }
        }
        return cronExpression;
    }

    // Following code is reported as duplicates since all the lines doing same function call.
    private static String buildCronExpression(BMap<BString, Object> record) {
        String cronExpression = getStringFieldValue(record, TaskConstants.FIELD_SECONDS) + " " +
                getStringFieldValue(record, TaskConstants.FIELD_MINUTES) + " " +
                getStringFieldValue(record, TaskConstants.FIELD_HOURS) + " " +
                getStringFieldValue(record, TaskConstants.FIELD_DAYS_OF_MONTH) + " " +
                getStringFieldValue(record, TaskConstants.FIELD_MONTHS) + " " +
                getStringFieldValue(record, TaskConstants.FIELD_DAYS_OF_WEEK) + " " +
                getStringFieldValue(record, TaskConstants.FIELD_YEAR);
        return cronExpression.trim();
    }

    private static String getStringFieldValue(BMap<BString, Object> record, BString fieldName) {
        if (TaskConstants.FIELD_DAYS_OF_MONTH.equals(fieldName) && Objects.isNull(record.get(TaskConstants.
                FIELD_DAYS_OF_MONTH))) {
            return "?";
        } else if (Objects.nonNull(record.get(fieldName))) {
            return record.get(fieldName).toString();
        } else {
            return "*";
        }
    }

    /*
     * TODO: Runtime validation is done as compiler plugin does not work right now.
     *       When compiler plugins can be run for the resources without parameters, this will be redundant.
     *       Issue: https://github.com/ballerina-platform/ballerina-lang/issues/14148
     */
    public static void validateService(ServiceInformation serviceInformation) throws SchedulingException {
        AttachedFunctionType[] resources = serviceInformation.getService().getType().getAttachedFunctions();
        if (resources.length != VALID_RESOURCE_COUNT) {
            throw new SchedulingException("Invalid number of resources found in service \'" +
                    serviceInformation.getServiceName() + "\'. Task service should include only one resource.");
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
        String name = service.getType().getName().split("\\$\\$")[1].replace("$_", "");
        String str = "";
        name = str.toUpperCase(Locale.forLanguageTag(name.substring(0, 1))) + name.substring(1);
        return name;
    }

    public static Properties createSchedulerProperties(BMap<BString, Object> configurations) {
        String uniqueID = UUID.randomUUID().toString();
        int thresholdInMillis = configurations.getIntValue(TaskConstants.THRESHOLD_IN_MILLIS).intValue();
        Properties properties = new Properties();
        properties.setProperty(TaskConstants.QUARTZ_MISFIRE_THRESHOLD, String.valueOf(thresholdInMillis));
        properties.setProperty(TaskConstants.QUARTZ_THREAD_COUNT, QUARTZ_THREAD_COUNT_VALUE);
        properties.setProperty(TaskConstants.QUARTZ_INSTANCE_NAME, String.valueOf(uniqueID));
        return properties;
    }

    public static Trigger createTrigger(SimpleScheduleBuilder scheduleBuilder, String name, long delay) {
        Trigger trigger;
        if (delay > 0) {
            Date startTime = new Date(System.currentTimeMillis() + delay);
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(name, name)
                    .startAt(startTime)
                    .withSchedule(scheduleBuilder)
                    .build();
        } else {
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(name, name)
                    .startNow()
                    .withSchedule(scheduleBuilder)
                    .build();
        }
        return trigger;
    }

    public static SimpleScheduleBuilder createSchedulerBuilder(BMap<BString, Object> configurations) {
        long interval = configurations.getIntValue(TaskConstants.FIELD_INTERVAL).intValue();
        long maxRuns = getMaxRuns(configurations);
        String policy = String.valueOf(configurations.getStringValue(TaskConstants.MISFIRE_POLICY));
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(interval);
        if (maxRuns > 0) {
            // Quartz uses number of repeats but total number of runs are counted here.
            // Hence, 1 is subtracted from the `maxRuns` to get the repeat count.
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

    public static CronTrigger createCronTrigger(CronScheduleBuilder scheduleBuilder, String name, long maxRuns) {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, name)
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

    public static long getMaxRuns(BMap<BString, Object> configurations) {
        long maxRuns;
        if (configurations.getIntValue(TaskConstants.FIELD_NO_OF_RUNS) == null) {
            maxRuns = 0;
        } else {
            maxRuns = configurations.getIntValue(TaskConstants.FIELD_NO_OF_RUNS).intValue();
        }
        return maxRuns;
    }
}
