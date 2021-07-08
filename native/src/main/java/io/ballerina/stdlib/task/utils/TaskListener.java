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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.objects.TaskManager;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;

import java.util.Map;

/**
 * The class to get the notification when a trigger fires.
 *
 * @since 2.0.0
 */
public class TaskListener implements TriggerListener {

    private static final String TRIGGER_LISTENER_NAME = "TaskListener";

    @Override
    public String getName() {
        return TRIGGER_LISTENER_NAME;
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {

    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        Map<Integer, Trigger> triggerInfo = TaskManager.getInstance().getTriggerInfoMap();
        Integer jobId = null;
        for (Map.Entry<Integer, Trigger> key : triggerInfo.entrySet()) {
            if (key.getValue().getKey() == trigger.getKey()) {
                jobId = key.getKey();
                break;
            }
        }
        BString msg = StringUtils.fromString("message = Ignore the trigger as couldn't get the resource to " +
                "execute the job[" + jobId +   "] at " + trigger.getStartTime());
        AbstractLogFunction.logMessage(msg, TaskConstants.PACKAGE_PATH,
                (pkg, message) -> {
                    AbstractLogFunction.getLogger(pkg).info(message);
                },
                TaskConstants.FORMAT);
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context,
                                Trigger.CompletedExecutionInstruction triggerInstructionCode) {

    }
}
