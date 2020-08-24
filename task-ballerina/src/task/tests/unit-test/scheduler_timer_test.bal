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

service timerService1 = service {
    resource function onTrigger(Person person) {
        person.age = person.age + 1;
        result = <@untainted string>(person.name + " is " + person.age.toString() + " years old");
    }
};

service service1 = service {
    resource function onTrigger() {
        firstTimerServiceTriggeredCount = firstTimerServiceTriggeredCount + 1;
        if (firstTimerServiceTriggeredCount > 3) {
            firstTimerServiceTriggered = true;
        }
    }
};

service service2 = service {
    resource function onTrigger() {
        secondTimerServiceTriggeredCount = secondTimerServiceTriggeredCount + 1;
        if (secondTimerServiceTriggeredCount > 3) {
            secondTimerServiceTriggered = true;
        }
    }
};

//@test:Config {}
//function testTaskTimerWithAttachment() {
//    Person person = {
//        name: "Sam",
//        age: 0
//    };
//
//    Scheduler taskTimer = new ({intervalInMillis: 1000, initialDelayInMillis: 1000, noOfRecurrences: 5});
//    var attachResult = taskTimer.attach(timerService1, person);
//    if (attachResult is SchedulerError) {
//        panic attachResult;
//    }
//    var startResult = taskTimer.start();
//    if (startResult is SchedulerError) {
//        panic startResult;
//    }
//    // Sleep for 8 seconds to check whether the task is running for more than 5 times.
//    runtime:sleep(8000);
//    checkpanic taskTimer.stop();
//    test:assertEquals(result, "Sam is 5 years old", msg = "Response payload mismatched");
//}

@test:Config {}
function testTaskTimerWithMultipleServices() {
    Scheduler timerWithMultipleServices = new ({intervalInMillis: 1000});
    checkpanic timerWithMultipleServices.attach(service1);
    checkpanic timerWithMultipleServices.attach(service2);
    checkpanic timerWithMultipleServices.start();
    runtime:sleep(5000);
    checkpanic timerWithMultipleServices.stop();
    test:assertTrue(firstTimerServiceTriggered, msg = "Response payload mismatched");
    test:assertTrue(secondTimerServiceTriggered, msg = "Response payload mismatched");
}
