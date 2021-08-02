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

package io.ballerina.stdlib.task.utils;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * Task related constants.
 */
public class TaskConstants {

    // Fields related to TaskError record
    public static final String ERROR = "Error";

    // Quarts property names
    public static final String QUARTZ_THREAD_COUNT = "org.quartz.threadPool.threadCount";
    public static final String QUARTZ_MISFIRE_THRESHOLD = "org.quartz.jobStore.misfireThreshold";

    // Quartz property values.
    public static final String QUARTZ_THREAD_COUNT_VALUE = "5";
    public static final String QUARTZ_THRESHOLD_VALUE = "5000";

    public static final String JOB = "job";
    public static final String FORMAT = "logfmt";
    public static final String JOB_ID = "jobId";
    public static final String ERROR_POLICY = "errorPolicy";
    public static final String TRIGGER_ID = "trigger";
    public static final String LOG_AND_IGNORE = "LOG_AND_IGNORE";
    public static final String EXECUTE = "execute";
    public static final String LOG = "log";
    public static final String LOG_AND_TERMINATE = "LOG_AND_TERMINATE";
    public static final String LOG_AND_CONTINUE = "LOG_AND_CONTINUE";
    public static final String TERMINATE = "TERMINATE";
    public static final String WAIT = "WAIT";
    public static final String PACKAGE_PATH = ".";
    public static final String LOG_FILE_PATH = "../native/src/main/resources/log4j2.properties";

    public static final BString ERR_POLICY = StringUtils.fromString("errorPolicy");
    public static final BString WAITING_POLICY = StringUtils.fromString("waitingPolicy");

    private TaskConstants() {

    }
}
