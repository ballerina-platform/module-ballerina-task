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

const APPOINTMENT_MULTI_SERVICE_SUCCESS_RESPONSE = "Multiple services invoked";
const LIMITED_RUNS_SUCCESS_RESPONSE = "Scheduler triggered limited times";
const APPOINTMENT_FAILURE_RESPONSE = "Services failed to trigger";

boolean firstTriggered = false;
boolean secondTriggered = false;
int triggerCount1 = 0;
int triggerCount2 = 0;
int triggerCount3 = 0;

function cronTriggerJob1() {
    triggerCount1 += 1;
    if (triggerCount1 > 3) {
        firstTriggered = true;
    }
}

function cronTriggerJob2() {
    triggerCount2 += 1;
    if (triggerCount2 > 3) {
        secondTriggered = true;
    }
}

function cronTriggerJob3() {
    triggerCount3 += 1;
}

@test:Config {}
function testSchedulerWithMultipleServices() {
    string cronExpression = "* * * * * ? *";
    Scheduler appointment = scheduler();
    var attachResult = appointment.scheduleJob(cronTriggerJob1, {cronExpression: cronExpression});
    var attachResult1 = appointment.scheduleJob(cronTriggerJob2, {cronExpression: cronExpression});
    runtime:sleep(4000);
    checkpanic appointment.stop();
    test:assertTrue(firstTriggered, msg = "Expected value mismatched");
    test:assertTrue(secondTriggered, msg = "Expected value mismatched");
}

@test:Config {}
function testLimitedNumberOfRuns() {
    string cronExpression = "* * * * * ? *";
    CronTriggerConfiguration configuration = {
        cronExpression: cronExpression,
        noOfRecurrences: 3
    };
    Scheduler appointmentWithLimitedRuns = scheduler();
    var result = appointmentWithLimitedRuns.scheduleJob(cronTriggerJob3, configuration);
    runtime:sleep(5000);
    checkpanic appointmentWithLimitedRuns.stop();
    test:assertEquals(triggerCount3, 3, msg = "Expected value mismatched");
}
