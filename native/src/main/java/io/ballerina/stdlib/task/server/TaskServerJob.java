/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.task.server;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.task.objects.TaskManager;
import io.ballerina.stdlib.task.utils.TaskConstants;
import io.ballerina.stdlib.task.utils.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class TaskServerJob implements Job {

    public TaskServerJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Thread.startVirtualThread(() -> {
            Runtime runtime = TaskManager.getInstance().getRuntime();
            BObject job = (BObject) jobExecutionContext.getMergedJobDataMap().get(TaskConstants.JOB);
            executeJob(job, runtime, jobExecutionContext);
        });
    }

    private void executeJob(BObject job, Runtime runtime, JobExecutionContext jobExecutionContext) {
        try {
            StrandMetadata metadata = new StrandMetadata(true, null);
            runtime.callMethod(job, TaskConstants.ON_TRIGGER, metadata);
        } catch (BError error) {
            Utils.notifyFailure(jobExecutionContext, error);
        } catch (Throwable t) {
            Utils.notifyFailure(jobExecutionContext, ErrorCreator.createError(t));
        }
    }
}
