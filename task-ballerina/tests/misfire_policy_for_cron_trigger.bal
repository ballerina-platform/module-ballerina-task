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

int triggeredCount6 = 0;

function cronMisfire6() {
    triggeredCount6 = triggeredCount6 + 1;
}

@test:Config {}
function testFireAndProceedWithService6() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(cronMisfire6, { cronExpression: "* * * * * ? *", noOfRecurrences: 8,
    misfirePolicy: "fireAndProceed"  });
    runtime:sleep(1500);
    int count = triggeredCount6;
    check taskTimer.pauseAllJobs();
    runtime:sleep(1000);
    test:assertEquals(triggeredCount6, count, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(6000);
    test:assertEquals(triggeredCount6, 8, msg = "Expected count mismatched.");
    check taskTimer.stop();
}

int triggeredCount7 = 0;

function cronMisfire7() {
    triggeredCount7 = triggeredCount7 + 1;
}

@test:Config {}
function testdoNothingWithService7() returns error? {
    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(cronMisfire7, { cronExpression: "* * * * * ? *", noOfRecurrences: 5,
    misfirePolicy: "doNothing" });
    runtime:sleep(1500);
    int count = triggeredCount7;
    check taskTimer.pauseAllJobs();
    runtime:sleep(1000);
    test:assertEquals(triggeredCount7, count, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resumeAllJobs();
    runtime:sleep(6000);
    test:assertEquals(triggeredCount7, 5, msg = "Expected count mismatched");
    check taskTimer.stop();
}
