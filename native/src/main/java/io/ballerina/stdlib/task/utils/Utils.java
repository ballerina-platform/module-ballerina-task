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

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.stdlib.task.exceptions.SchedulingException;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Utility functions used in ballerina task module.
 *
 * @since 0.995.0
 */
public class Utils {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static BError createTaskError(String message) {
        return ErrorCreator.createDistinctError(TaskConstants.ERROR, ModuleUtils.getModule(),
                StringUtils.fromString(message));
    }

    public static Properties createSchedulerProperties(String threadCount, String thresholdInMillis) {
        Properties properties = new Properties();
        properties.setProperty(TaskConstants.QUARTZ_MISFIRE_THRESHOLD, thresholdInMillis);
        properties.setProperty(TaskConstants.QUARTZ_THREAD_COUNT, threadCount);
        return properties;
    }

    public static Scheduler initializeScheduler(Properties properties) throws SchedulingException {
        try {
            StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory(properties);
            Scheduler scheduler = stdSchedulerFactory.getScheduler();
            scheduler.getListenerManager().addTriggerListener(new TaskListener(),
                    GroupMatcher.triggerGroupEquals(TaskConstants.LOG));
            return scheduler;
        } catch (SchedulerException e) {
            throw new SchedulingException("Cannot create the Scheduler." + e.getMessage());
        }
    }

    public static JobDetail createJob(JobDataMap jobDataMap, String jobId) {
        return JobBuilder.newJob(TaskJob.class).withIdentity(jobId).usingJobData(jobDataMap).build();
    }

    public static Trigger getOneTimeTrigger(long time, String triggerID) {
        String triggerKey = UUID.randomUUID().toString();
        Date startTime = new Date(time);
        return TriggerBuilder.newTrigger().withIdentity(triggerKey, triggerID).startAt(startTime).build();
    }

    public static Trigger getIntervalTrigger(long interval, long maxCount, Object startTime, Object endTime,
                                             String waitingPolicy, String triggerID) {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().
                withIntervalInMilliseconds(interval);
        setMaxCount(simpleScheduleBuilder, maxCount);
        setMisfire(simpleScheduleBuilder, waitingPolicy);
        if (waitingPolicy.equalsIgnoreCase(TaskConstants.LOG_AND_IGNORE)) {
            triggerID = TaskConstants.LOG;
        }
        return getTrigger(simpleScheduleBuilder, startTime, endTime, triggerID);
    }

    public static void setMisfire(SimpleScheduleBuilder simpleScheduleBuilder, String waitingPolicy) {
        if (TaskConstants.WAIT.equalsIgnoreCase(waitingPolicy)) {
            simpleScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
        }
    }

    public static void setMaxCount(SimpleScheduleBuilder simpleScheduleBuilder, long maxCount) {
        if (maxCount > 0) {
            simpleScheduleBuilder.withRepeatCount((int) (maxCount - 1));
        } else {
            simpleScheduleBuilder.repeatForever();
        }
    }

    public static Trigger getTrigger(SimpleScheduleBuilder simpleScheduleBuilder, Object startTime, Object endTime,
                                     String triggerID) {
        Date startDate;
        Date endDate;
        Trigger trigger;
        String triggerKey = UUID.randomUUID().toString();

        if (isInt(startTime) && isInt(endTime)) {
            startDate = new Date((Long) startTime);
            endDate = new Date((Long) endTime);
            trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey, triggerID).startAt(startDate).endAt(endDate).
                    withSchedule(simpleScheduleBuilder).build();
        } else if (isInt(startTime)) {
            startDate = new Date((Long) startTime);
            trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey, triggerID).startAt(startDate).
                    withSchedule(simpleScheduleBuilder).build();
        } else if (isInt(endTime)) {
            endDate = new Date((Long) endTime);
            trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey, triggerID).endAt(endDate).
                    withSchedule(simpleScheduleBuilder).build();
        } else {
            trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey, triggerID).
                    withSchedule(simpleScheduleBuilder).build();
        }
        return trigger;
    }

    public static void printMessage(String msg, PrintStream console) {
        OffsetDateTime utcNow = OffsetDateTime.now(ZoneOffset.UTC);
        console.println("time = " + formatter.format(utcNow) + " message = " + msg);
    }

    public static void disableQuartzLogs() {
        LogManager logManager = LogManager.getLogManager();
        Enumeration<String> names = logManager.getLoggerNames();
        for (String name : Collections.list(names)) {
            if (name.contains(TaskConstants.QUARTZ) || name.isEmpty()) {
                Logger logger = LogManager.getLogManager().getLogger(name);
                logger.setLevel(Level.OFF);
            }
        }
    }

    public static boolean isInt(Object time) {
        return TypeUtils.getType(time).getTag() == TypeTags.INT_TAG;
    }
}
