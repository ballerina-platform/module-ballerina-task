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
package org.ballerinalang.stdlib.task.actions;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;
import org.ballerinalang.stdlib.task.objects.TaskScheduler;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.utils.Utils;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * SchedulerActions implements the scheduler actions.
 *
 * @since 2.0.0
 */
public class SchedulerActions {

    @SuppressWarnings("unchecked") // It is used to ignore the unchecked generic types of operations warnings
    // from the `getMapValue` by the compilers.
    public static Object init(Environment env, BObject taskScheduler) {
        Runtime runtime = env.getRuntime();
        BMap<BString, Object> configurations = taskScheduler.getMapValue(TaskConstants.CONFIGURATION);
        try {
            StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory(
                    createSchedulerProperties(configurations));
            Scheduler scheduler = stdSchedulerFactory.getScheduler();
            TaskScheduler jobScheduler = new TaskScheduler(scheduler);
            taskScheduler.addNativeData(TaskConstants.SCHEDULER, jobScheduler);
            taskScheduler.addNativeData(TaskConstants.RUNTIME, runtime);
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked") // It is used to ignore the unchecked generic types of operations warnings
    // from the `getMapValue` by the compilers.
    public static Object scheduleJob(BObject taskScheduler, BObject job, BMap<BString, Object> config,
                                     Object... attachments) {
        Runtime runtime = (Runtime) taskScheduler.getNativeData(TaskConstants.RUNTIME);
        String jobId = String.valueOf(new Random().nextInt(1000000));
        JobDataMap jobDataMap = new JobDataMap();
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        if (!job.getType().getName().equals("Job")) {
            try {
                ServiceInformation serviceInformation = Utils.getServiceInformation(job, runtime, attachments);
                jobDataMap.put(TaskConstants.JOB, serviceInformation);
            } catch (SchedulingException e) {
                return Utils.createSchedulerError(e.getMessage());
            }
        } else {
            jobDataMap.put(TaskConstants.JOB, job);
            jobDataMap.put(TaskConstants.RUNTIME, runtime);
        }
        try {
            jobScheduler.addJob(jobDataMap, config, jobId);
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return StringUtils.fromString(jobId);
    }

    public static Object unScheduleJob(BObject taskScheduler, int id) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        try {
            jobScheduler.unScheduleJob(String.valueOf(id));
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Object pause(BObject taskScheduler) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        try {
            jobScheduler.pause();
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Object resume(BObject taskScheduler) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        try {
            jobScheduler.resume();
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Boolean isRunning(BObject taskScheduler) {
            TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
            try {
                return jobScheduler.isRunning();
            } catch (SchedulerException e) {
                return false;
            }
        }

    public static Object pauseJob(BObject taskScheduler, int id) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        try {
            jobScheduler.pauseJob(String.valueOf(id));
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static BArray getAllRunningJobs(BObject taskScheduler) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        Set<String> ids = jobScheduler.getAllRunningJobs();
        return StringUtils.fromStringSet(ids);
    }

    public static Object resumeJob(BObject taskScheduler, int id) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        try {
            jobScheduler.resumeJob(String.valueOf(id));
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Object rescheduleJob(BObject taskScheduler, int jobId, BMap<BString, Object> config) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        try {
            Trigger trigger = Utils.getTrigger(config, "trigger");
            jobScheduler.rescheduleJob(String.valueOf(jobId), trigger);
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Object deleteJob(BObject taskScheduler, int id) {
        TaskScheduler jobScheduler = (TaskScheduler) taskScheduler.getNativeData(TaskConstants.SCHEDULER);
        try {
            jobScheduler.deleteJob(String.valueOf(id));
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Object start(BObject taskListener) {
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.start();
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Object stop(BObject taskListener) {
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.stop();
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Object gracefulStop(BObject taskListener) {
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.gracefulStop();
        } catch (SchedulerException e) {
            return Utils.createSchedulerError(e.getMessage());
        }
        return null;
    }

    public static Properties createSchedulerProperties(BMap<BString, Object> configurations) {
        String uniqueID = UUID.randomUUID().toString();
        int thresholdInMillis = configurations.getIntValue(TaskConstants.THRESHOLD_IN_MILLIS).intValue();
        int threadCount = configurations.getIntValue(TaskConstants.THREAD_COUNT).intValue();
        int threadPriority = configurations.getIntValue(TaskConstants.THREAD_PRIORITY).intValue();
        Properties properties = new Properties();
        properties.setProperty(TaskConstants.QUARTZ_MISFIRE_THRESHOLD, String.valueOf(thresholdInMillis));
        properties.setProperty(TaskConstants.QUARTZ_THREAD_COUNT, String.valueOf(threadCount));
        properties.setProperty(TaskConstants.QUARTZ_THREAD_PRIORITY, String.valueOf(threadPriority));
        properties.setProperty(TaskConstants.QUARTZ_INSTANCE_NAME, String.valueOf(uniqueID));
        return properties;
    }
}
