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
package org.ballerinalang.stdlib.task.actions;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.objects.TaskManager;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.utils.Utils;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Set;

import static io.ballerina.runtime.api.creators.ValueCreator.createArrayValue;

/**
 * Class to handle ballerina external functions in Task library.
 *
 * @since 0.1.4
 */
public class TaskActions {

    private static int bound = 1000000;
    private static String value = "1000";

    public static Object configureThread(Environment env, long workerCount, long waitingTimeInMillis) {
        try {
            TaskManager.getInstance().initializeScheduler(Utils.createSchedulerProperties(
                    String.valueOf(workerCount), String.valueOf(waitingTimeInMillis)), env);
            return null;
        } catch (SchedulingException | SchedulerException e) {
            return Utils.createTaskError(e.getMessage());
        }
    }

    public static Object scheduleJob(Environment env, BObject job, long time) {
        Integer jobId = new Random().nextInt(bound);
        JobDataMap jobDataMap = getJobDataMap(job, TaskConstants.LOG_AND_CONTINUE, String.valueOf(jobId));
        try {
            getScheduler(env);
            TaskManager.getInstance().scheduleOneTimeJob(jobDataMap, time, jobId);
        } catch (SchedulerException | SchedulingException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return jobId;
    }

    public static Object scheduleIntervalJob(Environment env, BObject job, BDecimal interval, long maxCount,
                                             Object startTime, Object endTime, BMap<BString, Object> policy) {
        int jobId = new Random().nextInt(bound);
        JobDataMap jobDataMap = getJobDataMap(job, ((BString) policy.get(TaskConstants.ERR_POLICY)).getValue(),
                String.valueOf(jobId));
        try {
            getScheduler(env);
            TaskManager.getInstance().scheduleIntervalJob(jobDataMap,
                    (interval.decimalValue().multiply(new BigDecimal(value))).longValue(), maxCount, startTime,
                    endTime, ((BString) policy.get(TaskConstants.WAITING_POLICY)).getValue(), jobId);
        } catch (SchedulerException | SchedulingException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return jobId;
    }

    private static Scheduler getScheduler(Environment env) throws SchedulingException, SchedulerException {
        return TaskManager.getInstance().getScheduler(Utils.createSchedulerProperties(
                TaskConstants.QUARTZ_THREAD_COUNT_VALUE, TaskConstants.QUARTZ_THRESHOLD_VALUE), env);
    }

    private static JobDataMap getJobDataMap(BObject job, String errorPolicy, String jobId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TaskConstants.JOB, job);
        jobDataMap.put(TaskConstants.ERROR_POLICY, errorPolicy);
        jobDataMap.put(TaskConstants.JOB_ID, jobId);
        return jobDataMap;
    }

    public static Object unscheduleJob(Long jobId) {
        try {
            TaskManager.getInstance().unScheduleJob(Math.toIntExact(jobId));
        } catch (SchedulerException | SchedulingException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object pauseAllJobs() {
        try {
            TaskManager.getInstance().pause();
        } catch (SchedulerException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object resumeAllJobs() {
        try {
            TaskManager.getInstance().resume();
        } catch (SchedulerException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object pauseJob(Long jobId) {
        try {
            TaskManager.getInstance().pauseJob(Math.toIntExact(jobId));
        } catch (SchedulerException | SchedulingException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object resumeJob(Long jobId) {
        try {
            TaskManager.getInstance().resumeJob(Math.toIntExact(jobId));
        } catch (SchedulerException | SchedulingException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }

    public static BArray getRunningJobs() {
        try {
            int i = 0;
            Set<Integer> jobIds = TaskManager.getInstance().getAllRunningJobs();
            long[] results = new long[jobIds.size()];
            for (int value : jobIds) {
                results[i] = value;
                i++;
            }
            return ValueCreator.createArrayValue(results);
        } catch (SchedulerException e) {
            Utils.logError(StringUtils.fromString(e.toString()));
        }
        return createArrayValue(new long[0]);
    }
}
