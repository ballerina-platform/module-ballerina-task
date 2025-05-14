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

package io.ballerina.stdlib.task.listener;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.objects.TaskManager;
import io.ballerina.stdlib.task.utils.ModuleUtils;
import io.ballerina.stdlib.task.utils.Utils;
import org.quartz.SchedulerException;

import java.util.Map;

import static io.ballerina.stdlib.task.utils.TaskConstants.JOB_ID;

public class ListenerAction {
    private static final String NATIVE_LISTENER_KEY = "TASK_NATIVE_LISTENER";
    private static final String TIME_CONFIGURATION = "Civil";
    private static final String LISTENER_NOT_INITIALIZED_ERROR = "Listener not initialized";
    private static final String GET_TIME_IN_MILLIES = "getTimeInMillies";
    private static final long WORKER_COUNT = 5;
    private static final long WAITING_TIME_IN_MILLISECONDS = 5;
    private static final BString SCHEDULE = StringUtils.fromString("schedule");
    private static final BString INTERVAL = StringUtils.fromString("interval");
    private static final BString MAX_COUNT = StringUtils.fromString("maxCount");
    private static final BString START_TIME = StringUtils.fromString("startTime");
    private static final BString END_TIME = StringUtils.fromString("endTime");
    private static final BString TASK_POLICY = StringUtils.fromString("taskPolicy");
    private static final BString SCHEDULED_TIME = StringUtils.fromString("timeCivil");

    public static Object initListener(Environment env, BObject listener, BMap<BString, Object> listenerConfig) {
        BMap<?, ?> schedule = listenerConfig.getMapValue(SCHEDULE);
        TaskListener taskListener = new TaskListener(TaskManager.getInstance(), TypeUtils.getType(schedule).getName());
        if (TypeUtils.getType(schedule).getName().contains(TIME_CONFIGURATION)) {
            StrandMetadata metadata = new StrandMetadata(true, null);
            long scheduledTime = (long) env.getRuntime()
                    .callFunction(ModuleUtils.getModule(), GET_TIME_IN_MILLIES, metadata, schedule);
            taskListener.setConfig(SCHEDULED_TIME, scheduledTime);
        } else {
            taskListener.setConfigs(schedule);
        }
        listener.addNativeData(NATIVE_LISTENER_KEY, taskListener);
        return null;
    }

    public static Object start(Environment environment, BObject listenerObj) {
        TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
        try {
            if (listener != null) {
                if (listener.getType().equals(TIME_CONFIGURATION)) {
                    long triggerTime = (long) listener.getConfig().get(SCHEDULED_TIME);
                    listener.start(environment, listenerObj, triggerTime);
                } else {
                    listener.start(environment, listenerObj, (BDecimal) listener.getConfig().get(INTERVAL),
                                   (Long) listener.getConfig().get(MAX_COUNT), listener.getConfig().get(START_TIME),
                                   listener.getConfig().get(END_TIME), (BMap) listener.getConfig().get(TASK_POLICY));
                }
            } else {
                return Utils.createTaskError(LISTENER_NOT_INITIALIZED_ERROR);
            }
        } catch (Exception e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object attachService(BObject listenerObj, BObject service, BString serviceName) {
        TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
        service.addNativeData(JOB_ID, serviceName);
        listener.registerService(serviceName.getValue(), service);
        return null;
    }

    public static Object detach(BObject listenerObj, BObject service) {
        try {
            TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
            listener.unregisterService(service);
            return null;
        } catch (SchedulerException e) {
            return Utils.createTaskError(e.getMessage());
        }
    }

    public static Object gracefulStop(BObject listenerObj) {
        try {
            TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
            Map<String, BObject> services = listener.getServices();
            for (String entry : services.keySet()) {
                listener.getTaskManager().unScheduleJob(entry);
            }
            listener.unregisterAllServices();
        } catch (Exception e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object pauseAllJobs() {
        try {
            TaskManager.getInstance().pause();
            return null;
        } catch (SchedulerException e) {
            return Utils.createTaskError(e.getMessage());
        }
    }

    public static Object resumeAllJobs() {
        try {
            TaskManager.getInstance().resume();
            return null;
        } catch (SchedulerException e) {
            return Utils.createTaskError(e.getMessage());
        }
    }

    public static Object pauseService(BString serviceId) {
        try {
            TaskManager.getInstance().pauseJob(serviceId.getValue());
            return null;
        } catch (Exception e) {
            return Utils.createTaskError(e.getMessage());
        }
    }

    public static Object resumeService(BString serviceId) {
        try {
            TaskManager.getInstance().resumeJob(serviceId.getValue());
            return null;
        } catch (Exception e) {
            return Utils.createTaskError(e.getMessage());
        }
    }

    public static Object getRunningServices() {
        try {
            return TaskManager.getInstance().getAllRunningServices();
        } catch (SchedulerException e) {
            return Utils.createTaskError(e.getMessage());
        }
    }
}

