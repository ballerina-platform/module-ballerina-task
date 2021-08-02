/*
 *  Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.values.BError;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * The class handles the output of job execution by the trigger.
 *
 * @since 2.0.0
 */
public class TaskCallBack implements Callback {

    JobExecutionContext jobExecutionContext;

    public TaskCallBack(JobExecutionContext jobExecutionContext) {
        this.jobExecutionContext = jobExecutionContext;
    }

    @Override
    public void notifySuccess(Object o) {
    }

    @Override
    public void notifyFailure(BError bError) {
        Scheduler scheduler = jobExecutionContext.getScheduler();
        String errorPolicy = (String) jobExecutionContext.getMergedJobDataMap().get(TaskConstants.ERROR_POLICY);
        String jobId = (String) jobExecutionContext.getMergedJobDataMap().get(TaskConstants.JOB_ID);
        if (isLogged(errorPolicy)) {
            Utils.logError("message = Unable to execute the job[" + jobId + "]. " + bError.getMessage());
        }
        if (isTerminated(errorPolicy)) {
            try {
                scheduler.unscheduleJob(this.jobExecutionContext.getTrigger().getKey());
            } catch (SchedulerException e) {
                if (errorPolicy.equalsIgnoreCase(TaskConstants.LOG_AND_TERMINATE)) {
                    Utils.logError(e.toString());
                }
            }
        }
    }

    private boolean isLogged(String errorPolicy) {
        return errorPolicy.equalsIgnoreCase(TaskConstants.LOG_AND_TERMINATE) ||
                errorPolicy.equalsIgnoreCase(TaskConstants.LOG_AND_CONTINUE);
    }

    private boolean isTerminated(String errorPolicy) {
        return errorPolicy.equalsIgnoreCase(TaskConstants.LOG_AND_TERMINATE) ||
                errorPolicy.equalsIgnoreCase(TaskConstants.TERMINATE);
    }
}
