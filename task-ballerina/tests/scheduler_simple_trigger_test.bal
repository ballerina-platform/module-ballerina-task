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

boolean firstTimerServiceTriggered = false;
boolean secondTimerServiceTriggered = false;

string result = "";
int firstTimerServiceTriggeredCount = 0;
int secondTimerServiceTriggeredCount = 0;

function job10() {
    firstTimerServiceTriggeredCount = firstTimerServiceTriggeredCount + 1;
    if (firstTimerServiceTriggeredCount > 3) {
        firstTimerServiceTriggered = true;
    }
}

function job11() {
    secondTimerServiceTriggeredCount = secondTimerServiceTriggeredCount + 1;
    if (secondTimerServiceTriggeredCount > 3) {
        secondTimerServiceTriggered = true;
    }
}

Person person = {
    name: "Sam",
    age: 0
};

function job9() {
    person.age = person.age + 1;
    result = <@untainted string>(person.name + " is " + person.age.toString() + " years old");
}

@test:Config {}
function testTaskTimerWithAttachment() {

    Scheduler taskTimer = scheduler();
    var attachResult = taskTimer.scheduleJob(job9, {intervalInMillis: 1000, initialDelayInMillis: 1000,
    noOfRecurrences: 5});
    // Sleep for 8 seconds to check whether the task is running for more than 5 times.
    runtime:sleep(8000);
    checkpanic taskTimer.stop();
    test:assertEquals(result, "Sam is 5 years old", msg = "Expected value mismatched");
}

@test:Config {}
function testTaskTimerWithMultipleServices() {
    Scheduler timerWithMultipleServices = scheduler();
    var attachResult = timerWithMultipleServices.scheduleJob(job10, {intervalInMillis: 1000});
    var attachResult1 = timerWithMultipleServices.scheduleJob(job11, {intervalInMillis: 1000});
    runtime:sleep(5000);
    checkpanic timerWithMultipleServices.stop();
    test:assertTrue(firstTimerServiceTriggered, msg = "Expected value mismatched");
    test:assertTrue(secondTimerServiceTriggered, msg = "Expected value mismatched");
}

boolean fourthTimerServiceTriggered = false;
int fourthTimerServiceTriggeredCount = 0;

function job12() {
    fourthTimerServiceTriggeredCount = fourthTimerServiceTriggeredCount + 1;
    if (fourthTimerServiceTriggeredCount > 3) {
        fourthTimerServiceTriggered = true;
    }
}

@test:Config {}
function testTaskTimerWithSameServices() {
    Scheduler timerWithMultipleServices = scheduler();
    var attachResult =  timerWithMultipleServices.scheduleJob(job12, {intervalInMillis: 1000});
    runtime:sleep(1200);
    var attachResult1 =  timerWithMultipleServices.scheduleJob(job12, {intervalInMillis: 1200});
    runtime:sleep(2500);
    checkpanic timerWithMultipleServices.stop();
    test:assertTrue(fourthTimerServiceTriggered, msg = "Expected value mismatched");
}

int triggeredCount19 = 0;

function job19() {
    triggeredCount19 = triggeredCount19 + 1;
}

@test:Config {}
function testTaskTimerWithSameServices19() {
    Scheduler timerWithMultipleServices = scheduler();
    var attachResult =  timerWithMultipleServices.scheduleJob(job19, {intervalInMillis: 1000});
    runtime:sleep(3500);
    checkpanic timerWithMultipleServices.stop();
    test:assertEquals(triggeredCount19, 3, msg = "Expected value mismatched");
}

int jobCount5 = 0;
function job5() {
    jobCount5 = jobCount5 + 1;
}

@test:Config {}
function testgetAllRunningJobs() {
    Scheduler testScheduler = scheduler();
    var id3 =  testScheduler.scheduleJob(job5, {intervalInMillis: 1000});
    var id4 =  testScheduler.scheduleJob(job5, {intervalInMillis: 1000});
    var id5 =  testScheduler.scheduleJob(job5, {intervalInMillis: 1000, noOfRecurrences : 2});
    runtime:sleep(3500);
    string[]|Error ids = testScheduler.getRunningJobs();
    checkpanic testScheduler.stop();
    if (ids is string[]) {
        test:assertEquals(ids.length(), 2, msg = "Expected value mismatched");
    }

}

isolated function job13() {
}

@test:Config {}
function testIsSchedulerStarting() {
    Scheduler testScheduler = scheduler();
    test:assertFalse(testScheduler.isRunning(), msg = "Scheduler is not started.");
    var id3 =  testScheduler.scheduleJob(job13, {intervalInMillis: 1000});
    runtime:sleep(500);
    test:assertTrue(testScheduler.isRunning(), msg = "Expected value mismatched");
    checkpanic testScheduler.stop();
}

int jobCount6 = 0;

function job6() {
    jobCount6 = jobCount6 + 1;
}

@test:Config {}
function testRescheduleJob() {
    Scheduler testScheduler = scheduler();
    var id =  testScheduler.scheduleJob(function() { jobCount6 = jobCount6 + 1; }, {intervalInMillis: 1000});
    runtime:sleep(3500);
    test:assertEquals(jobCount6, 3, msg = "Output mismatched");
    if (id is int) {
        checkpanic testScheduler.rescheduleJob(id, {intervalInMillis: 5000});
    }
    runtime:sleep(2500);
    test:assertEquals(jobCount6, 3, msg = "Output mismatched");
    runtime:sleep(3500);
    test:assertEquals(jobCount6, 4, msg = "Output mismatched");
    checkpanic testScheduler.stop();
}

int jobCount7 = 0;

function job7() {
    jobCount7 = jobCount7 + 1;
}

@test:Config {}
function testSchdulerConfig() {
    Scheduler testScheduler = scheduler (threadCount = 1, threadPriority = 1);
    var id3 =  testScheduler.scheduleJob(job7, {intervalInMillis: 1000});
    runtime:sleep(3500);
    checkpanic testScheduler.stop();
    test:assertEquals(jobCount7, 3, msg = "Expected value mismatched");
}
