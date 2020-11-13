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
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;
import org.ballerinalang.stdlib.task.objects.TaskManager;
import org.ballerinalang.stdlib.task.objects.TaskScheduler;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.utils.Utils;
import org.quartz.SchedulerException;

import java.util.UUID;

/**
 * Class to handle ballerina external functions in Task library.
 *
 * @since 0.1.4
 */
public class TaskActions {

    public static Object pause(BObject taskListener) {
        String triggerID = (String) taskListener.getNativeData(TaskConstants.TRIGGER_NAME);
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.pause(triggerID);
        } catch (SchedulerException e) {
            return Utils.createListenerError(e.getMessage());
        }
        return null;
    }

    public static Object resume(BObject taskListener) {
        String triggerID = (String) taskListener.getNativeData(TaskConstants.TRIGGER_NAME);
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.resume(triggerID);
        } catch (SchedulerException e) {
            return Utils.createListenerError(e.getMessage());
        }
        return null;
    }

    public static Object detach(BObject taskListener, BObject service) {
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.removeService(service);
        } catch (SchedulerException e) {
            return Utils.createListenerError(e.getMessage());
        }
        return null;
    }

    public static Object start(BObject taskListener) {
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.start();
        } catch (SchedulerException e) {
            return Utils.createListenerError(e.getMessage());
        }
        return null;
    }

    public static Object stop(BObject taskListener) {
        String triggerID = (String) taskListener.getNativeData(TaskConstants.TRIGGER_NAME);
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.stop(triggerID);
        } catch (SchedulerException e) {
            return Utils.createListenerError(e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked") // It is used to ignore the unchecked generic types of operations warnings
    // from the `getMapValue` by the compilers.
    public static Object attach(BObject taskListener, Object servi, Object... attachments) {
        Runtime runtime = (Runtime) taskListener.getNativeData(TaskConstants.RUNTIME);
        BObject service = (BObject) servi;
        String triggerID = (String) taskListener.getNativeData(TaskConstants.TRIGGER_NAME);
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        ServiceInformation serviceInformation;
        try {
            serviceInformation = Utils.getServiceInformation(service, runtime, attachments);
            BMap<BString, Object> configurations = taskListener.getMapValue(TaskConstants.TRIGGER_CONFIGURATION);
            taskScheduler.addService(serviceInformation, configurations, triggerID);
        } catch (SchedulingException | SchedulerException e) {
            return Utils.createListenerError(e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked") // It is used to ignore the unchecked generic types of operations warnings
    // from the `getMapValue` by the compilers.
    public static Object init(Environment env, BObject taskListener) {
        Runtime runtime = env.getRuntime();
        BMap<BString, Object> configurations = taskListener.getMapValue(TaskConstants.TRIGGER_CONFIGURATION);
        try {
            if (!TaskConstants.SIMPLE_TRIGGER_CONFIGURATION.equals(configurations.getType().getName())) {
                BString cronExpression = configurations.getStringValue(TaskConstants.MEMBER_CRON_EXPRESSION);
                Utils.validateCronExpression(cronExpression);
            }
            TaskScheduler taskScheduler = new TaskScheduler(TaskManager.getInstance().getScheduler());
            taskListener.addNativeData(TaskConstants.SCHEDULER, taskScheduler);
            taskListener.addNativeData(TaskConstants.TRIGGER_NAME, UUID.randomUUID().toString());
            taskListener.addNativeData(TaskConstants.RUNTIME, runtime);
        } catch (SchedulingException e) {
            return Utils.createListenerError(e.getMessage());
        }
        return null;
    }
}
