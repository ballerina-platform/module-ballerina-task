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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.objects.TaskManager;
import io.ballerina.stdlib.task.utils.ModuleUtils;
import io.ballerina.stdlib.task.utils.Utils;

import java.util.Map;

import static io.ballerina.stdlib.task.utils.TaskConstants.JOB_ID;

public class ListenerAction {
    public static final String NATIVE_LISTENER_KEY = "TASK_NATIVE_LISTENER";
    public static final BString INTERVAL = StringUtils.fromString("interval");
    public static final BString MAX_COUNT = StringUtils.fromString("maxCount");
    public static final BString START_TIME = StringUtils.fromString("startTime");
    public static final BString END_TIME = StringUtils.fromString("endTime");
    public static final BString TASK_POLICY = StringUtils.fromString("taskPolicy");
    public static final BString TRIGGER_TIME = StringUtils.fromString("triggerTime");
    public static final String ONE_TIME_CONFIGURATION = "OneTimeConfiguration";
    public static final String LISTENER_NOT_INITIALIZED_ERROR = "Listener not initialized";
    public static final String TIME_CONVERTER_CLASS = "TimeConverter";
    public static final String GET_TIME_IN_MILLIES = "getTimeInMillies";

    public static void initListener(Environment env, BObject listener, BMap<BString, Object> listenerConfig) {
        TaskListener taskListener = new TaskListener();
        BMap<?, ?> schedule = listenerConfig.getMapValue(StringUtils.fromString("schedule"));
        taskListener.setType(TypeUtils.getType(schedule).getName());
        if (TypeUtils.getType(schedule).getName().contains(ONE_TIME_CONFIGURATION)) {
            Object time = schedule.get(TRIGGER_TIME);
            StrandMetadata metadata = new StrandMetadata(true, null);
            BObject timeConverter =
                    ValueCreator.createObjectValue(ModuleUtils.getModule(), TIME_CONVERTER_CLASS);
            long triggerTime = (long) env.getRuntime().callMethod(timeConverter, GET_TIME_IN_MILLIES, metadata, time);
            taskListener.setConfig(TRIGGER_TIME, triggerTime);
        } else {
            taskListener.setConfigs(schedule);
        }
        listener.addNativeData(NATIVE_LISTENER_KEY, taskListener);
    }

    public static Object startListener(Environment environment, BObject listenerObj) {
        TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
        try {
            if (listener != null) {
                if (listener.getType().equals(ONE_TIME_CONFIGURATION)) {
                    long triggerTime = (long) listener.getConfig().get(TRIGGER_TIME);
                    listener.start(environment, listenerObj, triggerTime);
                } else {
                    listener.start(environment, listenerObj,
                            (BDecimal) listener.getConfig().get(INTERVAL),
                            (Long) listener.getConfig().get(MAX_COUNT),
                            listener.getConfig().get(START_TIME),
                            listener.getConfig().get(END_TIME),
                            (BMap) listener.getConfig().get(TASK_POLICY));
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

    public static Object detachService(BObject listenerObj, BObject service, BString serviceName) {
        TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
        listener.unregisterService(serviceName.getValue());
        return null;
    }

    public static Object immediateStopListener(Environment env, BObject listenerObj) {
        try {
            TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
            Map<String, BObject> services = listener.getServices();
            for (String entry : services.keySet()) {
                TaskManager.getInstance().unScheduleJob(entry);
            }
            listener.unregisterAllServices();
        } catch (Exception e) {
            return Utils.createTaskError(e.getMessage());
        }
        return null;
    }
}

