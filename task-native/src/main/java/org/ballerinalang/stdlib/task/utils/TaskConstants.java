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

package org.ballerinalang.stdlib.task.utils;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.StringUtils;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.util.BLangConstants;

/**
 * Task related constants.
 */
public class TaskConstants {

    // Package related constants
    public static final String PACKAGE_NAME = "task";
    public static final String PACKAGE_VERSION = "1.1.2";
    public static final Module TASK_PACKAGE_ID =
            new Module(BLangConstants.BALLERINA_BUILTIN_PKG_PREFIX, PACKAGE_NAME, PACKAGE_VERSION);

    // Record types used
    public static final String RECORD_TIMER_CONFIGURATION = "TimerConfiguration";
    static final String RECORD_APPOINTMENT_DATA = "AppointmentData";
    public static final String SCHEDULER = "scheduler";
    public static final String TRIGGER_NAME = "triggerName";

    // Member names used in records
    public static final BString MEMBER_LISTENER_CONFIGURATION = StringUtils.fromString("listenerConfiguration");
    public static final BString MEMBER_CRON_EXPRESSION = StringUtils.fromString("cronExpression");

    // Misfire instructions
    public static final String FIRE_NOW = "fireNow";
    public static final String IGNORE_POLICY = "ignorePolicy";
    public static final String NEXT_WITH_EXISTING_COUNT = "fireNextWithExistingCount";
    public static final String NEXT_WITH_REMAINING_COUNT = "fireNextWithRemainingCount";
    public static final String NOW_WITH_EXISTING_COUNT = "fireNowWithExistingCount";
    public static final String NOW_WITH_REMAINING_COUNT = "fireNowWithRemainingCount";
    public static final String DO_NOTHING = "doNothing";
    public static final String FIRE_AND_PROCEED = "fireAndProceed";

    // Fields used in the `MisfireConfiguration`.
    public static final BString THRESHOLD_IN_MILLIS = StringUtils.fromString("thresholdInMillis");
    public static final BString MISFIRE_POLICY = StringUtils.fromString("misfirePolicy");

    // Allowed resource function names.
    public static final String RESOURCE_ON_TRIGGER = "onTrigger";
    public static final String SERVICE_INFORMATION = "serviceInfo";

    // Common field for TimerConfiguration and AppointmentConfiguration
    public static final BString FIELD_NO_OF_RUNS = StringUtils.fromString("noOfRecurrences");

    // Fields used in TimerConfiguration
    public static final BString FIELD_INTERVAL = StringUtils.fromString("intervalInMillis");
    public static final BString FIELD_DELAY = StringUtils.fromString("initialDelayInMillis");

    // Fields related to TaskError record
    static final String LISTENER_ERROR = "ListenerError";

    // Quarts property names
    public static final String QUARTZ_THREAD_COUNT = "org.quartz.threadPool.threadCount";
    public static final String QUARTZ_MISFIRE_THRESHOLD = "org.quartz.jobStore.misfireThreshold";

    // Quartz property values.
    public static final String QUARTZ_THREAD_COUNT_VALUE = "10";
    public static final String QUARTZ_THRESHOLD_VALUE = "5000";

    TaskConstants() {

    }
}
