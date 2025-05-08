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
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.objects.TaskManager;
import io.ballerina.stdlib.task.utils.Utils;

import java.util.Map;

import static io.ballerina.stdlib.task.utils.TaskConstants.JOB_ID;

public class ListenerAction {
    private static final String NATIVE_LISTENER_KEY = "TASK_NATIVE_LISTENER";
    private static final String LISTENER_NOT_INITIALIZED_ERROR = "Listener not initialized";
    private static final BString TRIGGER = StringUtils.fromString("trigger");
    private static final BString INTERVAL = StringUtils.fromString("interval");
    private static final BString MAX_COUNT = StringUtils.fromString("maxCount");
    private static final BString START_TIME = StringUtils.fromString("startTime");
    private static final BString END_TIME = StringUtils.fromString("endTime");
    private static final BString TASK_POLICY = StringUtils.fromString("taskPolicy");
    public static final BString WARM_BACKUP_CONFIG = StringUtils.fromString("warmBackupConfig");

    private ListenerAction() { }

    public static Object initListener(BObject listener, BMap<BString, Object> listenerConfig) {
        BMap<?, ?> configs = listenerConfig.getMapValue(TRIGGER);
        TaskListener taskListener = new TaskListener(TaskManager.getInstance());
        taskListener.setConfigs(configs);

        BMap<?, ?> warmBackupConfig = listenerConfig.getMapValue(WARM_BACKUP_CONFIG);
        if (warmBackupConfig != null) {
            taskListener.setConfig(WARM_BACKUP_CONFIG, warmBackupConfig);
        }
        listener.addNativeData(NATIVE_LISTENER_KEY, taskListener);
        return null;
    }

    public static Object start(Environment environment, BObject listenerObj) {
        TaskListener listener = (TaskListener) listenerObj.getNativeData(NATIVE_LISTENER_KEY);
        try {
            if (listener != null) {
                if (listener.getType().equals(ONE_TIME_CONFIGURATION)) {
                    long triggerTime = (long) listener.getConfig().get(TRIGGER_TIME);
                    listener.start(environment, listenerObj, triggerTime);
                } else {
                    if (listener.getConfig().containsKey(WARM_BACKUP_CONFIG)) {
                        BMap warmBackupConfig = (BMap) listener.getConfig().get(WARM_BACKUP_CONFIG);
                        listener.start(environment, listenerObj,
                                (BDecimal) listener.getConfig().get(INTERVAL),
                                (Long) listener.getConfig().get(MAX_COUNT),
                                listener.getConfig().get(START_TIME),
                                listener.getConfig().get(END_TIME),
                                (BMap) listener.getConfig().get(TASK_POLICY), warmBackupConfig);
                    } else {
                        listener.start(environment, listenerObj,
                                (BDecimal) listener.getConfig().get(INTERVAL),
                                (Long) listener.getConfig().get(MAX_COUNT),
                                listener.getConfig().get(START_TIME),
                                listener.getConfig().get(END_TIME),
                                (BMap) listener.getConfig().get(TASK_POLICY));
                    }
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
        } catch (Exception e) {
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
}

