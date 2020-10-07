// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/runtime;
import ballerina/test;

int triggeredCount1 = 0;

service misfireService1 = service {
    resource function onTrigger() {
        triggeredCount1 = triggeredCount1 + 1;
    }
};

@test:Config {}
function testFireNowWithService1() {

    Scheduler taskTimer = new ({ intervalInMillis: 3000, noOfRecurrences: 1},
                               { policy: "fireNow" });
    var attachResult = taskTimer.attach(misfireService1);
    if (attachResult is SchedulerError) {
        panic attachResult;
    }
    var startResult = taskTimer.start();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    var pauseResult = taskTimer.pause();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    runtime:sleep(5000);
    test:assertEquals(triggeredCount1, 0, msg = "Output mismatched");
    var resumeResult = taskTimer.resume();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    runtime:sleep(2000);
    checkpanic taskTimer.stop();
    test:assertEquals(triggeredCount1, 1, msg = "Output mismatched");
}

int triggeredCount8 = 0;
service misfireService8 = service {
    resource function onTrigger() {
        triggeredCount8 = triggeredCount8 + 1;
    }
};
@test:Config {}
function testIgnoreMisfiresPoilcyWithService8() {
    Scheduler taskTimer = new ({ intervalInMillis: 3000, noOfRecurrences: 1 },
                               { policy: "ignorePolicy" });
    var attachResult = taskTimer.attach(misfireService8);
    if (attachResult is SchedulerError) {
        panic attachResult;
    }
    var startResult = taskTimer.start();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    var pauseResult = taskTimer.pause();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    runtime:sleep(5000);
    test:assertEquals(triggeredCount8, 0, msg = "Output mismatched");
    var resumeResult = taskTimer.resume();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    runtime:sleep(2000);
    checkpanic taskTimer.stop();
    test:assertEquals(triggeredCount8, 1, msg = "Output mismatched");
}

int triggeredCount9 = 0;

service misfireService9 = service {
    resource function onTrigger() {
        triggeredCount9 = triggeredCount9 + 1;
    }
};

@test:Config {}
function testSmartPolicyWithService9() {

    Scheduler taskTimer = new ({ intervalInMillis: 3000, noOfRecurrences: 1});
    var attachResult = taskTimer.attach(misfireService9);
    if (attachResult is SchedulerError) {
        panic attachResult;
    }
    var startResult = taskTimer.start();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    var pauseResult = taskTimer.pause();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    runtime:sleep(5000);
    test:assertEquals(triggeredCount9, 0, msg = "Output mismatched");
    var resumeResult = taskTimer.resume();
    if (startResult is SchedulerError) {
        panic startResult;
    }
    runtime:sleep(2000);
    checkpanic taskTimer.stop();
    test:assertEquals(triggeredCount9, 1, msg = "Output mismatched");
}
