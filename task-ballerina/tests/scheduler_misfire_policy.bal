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

int triggeredCount8 = 0;

function misfirejob8() {
    triggeredCount8 = triggeredCount8 + 1;
}

@test:Config {}
function testFireNowWithService1() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfirejob8,
    { intervalInMillis: 3000, noOfRecurrences: 1, misfirePolicy: "fireNow"});
    check taskTimer.pauseAllJobs();
    runtime:sleep(5000);
    test:assertEquals(triggeredCount8, 0, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(2000);
    check taskTimer.stop();
    test:assertEquals(triggeredCount8, 1, msg = "Expected count mismatched.");
}

int triggeredCount9 = 0;
function misfirejob9() {
    triggeredCount9 = triggeredCount9 + 1;
}

@test:Config {}
function testIgnoreMisfiresPolicyWithService8() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfirejob9,
    { intervalInMillis: 3000, noOfRecurrences: 1 , misfirePolicy: "ignorePolicy"});
    check taskTimer.pauseAllJobs();
    runtime:sleep(5000);
    test:assertEquals(triggeredCount9, 0, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(2000);
    check taskTimer.stop();
    test:assertEquals(triggeredCount9, 1, msg = "Expected count mismatched.");
}

int triggeredCount10 = 0;

function misfirejob10() {
    triggeredCount10 = triggeredCount10 + 1;
}

@test:Config {}
function testSmartPolicyWithService9() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfirejob10, { intervalInMillis: 3000, noOfRecurrences: 1});
    check taskTimer.pauseAllJobs();
    runtime:sleep(5000);
    test:assertEquals(triggeredCount10, 0, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(2000);
    check taskTimer.stop();
    test:assertEquals(triggeredCount10, 1, msg = "Expected count mismatched.");
}
