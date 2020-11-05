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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;
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
            return Utils.createTaskError("Cannot pause Listener/Scheduler." + e.getMessage());
        }
        return null;
    }

    public static Object resume(BObject taskListener) {
        String triggerID = (String) taskListener.getNativeData(TaskConstants.TRIGGER_NAME);
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.resume(triggerID);
        } catch (SchedulerException e) {
            return Utils.createTaskError("Cannot resume Listener/Scheduler." + e.getMessage());
        }
        return null;
    }

    public static Object detach(BObject taskListener, BObject service) {
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.removeService(service);
        } catch (SchedulerException e) {
            return Utils.createTaskError("Cannot detach the service to the Listener/Scheduler" + e.getMessage());
        }
        return null;
    }

    public static Object start(BObject taskListener) {
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.start();
        } catch (SchedulerException e) {
            return Utils.createTaskError("Cannot start Listener/Scheduler." + e.getMessage());
        }
        return null;
    }

    public static Object stop(BObject taskListener) {
        String triggerID = (String) taskListener.getNativeData(TaskConstants.TRIGGER_NAME);
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        try {
            taskScheduler.stop(triggerID);
        } catch (SchedulerException e) {
            return Utils.createTaskError("Cannot stop Listener/Scheduler." + e.getMessage());
        }
        return null;
    }

    public static Object attach(BObject taskListener, BObject service, Object... attachments) {
        String triggerID = (String) taskListener.getNativeData(TaskConstants.TRIGGER_NAME);
        TaskScheduler taskScheduler = (TaskScheduler) taskListener.getNativeData(TaskConstants.SCHEDULER);
        ServiceInformation serviceInformation;
        if (attachments == null) {
            serviceInformation = new ServiceInformation(Runtime.getCurrentRuntime(), service);
        } else {
            serviceInformation = new ServiceInformation(Runtime.getCurrentRuntime(), service, attachments);
        }
        try {
            Utils.validateService(serviceInformation);
            BMap<BString, Object> configurations = taskListener.getMapValue(TaskConstants.
                    MEMBER_LISTENER_CONFIGURATION);
            taskScheduler.addService(serviceInformation, configurations, triggerID);
        } catch (SchedulingException e) {
            return Utils.createTaskError(e.getMessage());
        } catch (SchedulerException e) {
            return Utils.createTaskError("Cannot attach the service to the Listener/Scheduler." + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Object init(BObject taskListener) {
        BMap<BString, Object> configurations = taskListener.getMapValue(TaskConstants.MEMBER_LISTENER_CONFIGURATION);
        try {
            if (!TaskConstants.RECORD_TIMER_CONFIGURATION.equals(configurations.getType().getName())) {
                Object cronExpression = configurations.get(TaskConstants.MEMBER_CRON_EXPRESSION);
                Utils.validateCronExpression(cronExpression);
            }
            TaskScheduler taskScheduler = new TaskScheduler();
            taskListener.addNativeData(TaskConstants.SCHEDULER, taskScheduler);
            taskListener.addNativeData(TaskConstants.TRIGGER_NAME, UUID.randomUUID().toString());
        } catch (SchedulingException e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }
}
