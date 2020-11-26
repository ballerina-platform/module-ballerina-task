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

int triggeredCount2 = 0;

function misfireJob2() {
    triggeredCount2 = triggeredCount2 + 1;
}

@test:Config {}
function testExistingRepeatCountWithService2() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfireJob2, { intervalInMillis: 3000, noOfRecurrences: 5 ,
    misfirePolicy: "fireNowWithExistingCount" });
     // Sleep for 8 seconds.
    runtime:sleep(8000);
    test:assertEquals(triggeredCount2, 2, msg = "Expected count mismatched before pausing the scheduler.");
    check taskTimer.pauseAllJobs();
    runtime:sleep(8000);
    test:assertEquals(triggeredCount2, 2, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(1000);
    test:assertEquals(triggeredCount2, 3, msg = "Expected count mismatched after resuming the scheduler.");
    runtime:sleep(8000);
    test:assertEquals(triggeredCount2, 5, msg = "Expected count mismatched.");
    check taskTimer.stop();
}

int triggeredCount3 = 0;

function misfireJob3() {
    triggeredCount3 = triggeredCount3 + 1;
}

@test:Config {}
function testNowWithRemainingRepeatCountWithService3() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfireJob3, { intervalInMillis: 3000, noOfRecurrences: 7,
    misfirePolicy: "fireNowWithRemainingCount"});
    runtime:sleep(8000);
    test:assertEquals(triggeredCount3, 2, msg = "Expected count mismatched before pausing the scheduler.");
    check taskTimer.pauseAllJobs();
    runtime:sleep(8000);
    test:assertEquals(triggeredCount3, 2, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(1000);
    test:assertEquals(triggeredCount3, 3, msg = "Expected count mismatched after resuming the scheduler.");
    runtime:sleep(8000);
    test:assertEquals(triggeredCount3, 5, msg = "Expected count mismatched.");
    check taskTimer.stop();
}

int triggeredCount4 = 0;

function misfireJob4() {
    triggeredCount4 = triggeredCount4 + 1;
}

@test:Config {}
function testNextWithExistingCountWithService4() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfireJob4, { intervalInMillis: 3000, noOfRecurrences: 7,
    misfirePolicy: "fireNextWithExistingCount"});
    runtime:sleep(7000);
    test:assertEquals(triggeredCount4, 2, msg = "Expected count mismatched before pausing the scheduler.");
    check taskTimer.pauseAllJobs();
    runtime:sleep(7000);
    test:assertEquals(triggeredCount4, 2, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(500);
    test:assertEquals(triggeredCount4, 2, msg = "Expected count mismatched after resuming the scheduler.");
    runtime:sleep(10000);
    test:assertEquals(triggeredCount4, 5, msg = "Expected count mismatched.");
    check taskTimer.stop();
}

int triggeredCount5 = 0;

function misfireJob5() {
    triggeredCount5 = triggeredCount5 + 1;
}

@test:Config {}
function testNextWithRemainigCountWithService5() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfireJob5, { intervalInMillis: 3000, noOfRecurrences: 7 ,
    misfirePolicy: "fireNextWithExistingCount"});
    runtime:sleep(7000);
    test:assertEquals(triggeredCount5, 2, msg = "Expected count mismatched before pausing the scheduler.");
    check taskTimer.pauseAllJobs();
    runtime:sleep(7000);
    test:assertEquals(triggeredCount5, 2, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(500);
    test:assertEquals(triggeredCount5, 2, msg = "Expected count mismatched after resuming the scheduler.");
    runtime:sleep(10000);
    test:assertEquals(triggeredCount5, 5, msg = "Expected count mismatched.");
    check taskTimer.stop();
}

int triggeredCount11 = 0;

function misfireJob11() {
    triggeredCount11 = triggeredCount11 + 1;
}

@test:Config {}
function testSmartPolicyWithService11() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(misfireJob11, {intervalInMillis: 3000, noOfRecurrences: 5 });
    runtime:sleep(8000);
    test:assertEquals(triggeredCount11, 2, msg = "Expected count mismatched before pausing the scheduler.");
    check taskTimer.pauseAllJobs();
    runtime:sleep(8000);
    test:assertEquals(triggeredCount11, 2, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(1000);
    test:assertEquals(triggeredCount11, 3, msg = "Expected count mismatched after resuming the scheduler.");
    runtime:sleep(10000);
    test:assertEquals(triggeredCount11, 5, msg = "Expected count mismatched.");
    check taskTimer.stop();
}
