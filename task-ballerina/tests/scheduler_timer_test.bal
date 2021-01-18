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

import ballerina/lang.runtime as runtime;
import ballerina/test;

boolean firstTimerServiceTriggered = false;
boolean secondTimerServiceTriggered = false;

string result = "";
int firstTimerServiceTriggeredCount = 0;
int secondTimerServiceTriggeredCount = 0;

service object {} service1 = service object {
    remote function onTrigger() {
        firstTimerServiceTriggeredCount = firstTimerServiceTriggeredCount + 1;
        if (firstTimerServiceTriggeredCount > 3) {
            firstTimerServiceTriggered = true;
        }
    }
};

service object {} service2 = service object {
    remote function onTrigger() {
        secondTimerServiceTriggeredCount = secondTimerServiceTriggeredCount + 1;
        if (secondTimerServiceTriggeredCount > 3) {
            secondTimerServiceTriggered = true;
        }
    }
};

service object {} timerService1 = service object {
    remote function onTrigger(Person person) {
        person.age = person.age + 1;
        result = <@untainted string>(person.name + " is " + person.age.toString() + " years old");
    }
};

@test:Config {}
function testTaskTimerWithAttachment() {
    Person person = {
        name: "Sam",
        age: 0
    };

    Scheduler|SchedulerError taskTimer = new ({intervalInMillis: 1000, initialDelayInMillis: 1000, noOfRecurrences: 5});
    if (taskTimer is Scheduler) {
        var attachResult = taskTimer.attach(timerService1, person);

        if (attachResult is SchedulerError) {
            panic attachResult;
        }
        var startResult = taskTimer.start();
        if (startResult is SchedulerError) {
            panic startResult;
        }
        // Sleep for 8 seconds to check whether the task is running for more than 5 times.
        runtime:sleep(8);
        var stopResult = taskTimer.stop();
        test:assertEquals(result, "Sam is 5 years old", msg = "Expected value mismatched");
    } else {
        test:assertFail("Test failed due to an error in the creating of the scheduler.");
    }
}

@test:Config {}
function testTaskTimerWithMultipleServices() {
    Scheduler|SchedulerError timerWithMultipleServices = new ({intervalInMillis: 1000});
    if (timerWithMultipleServices is Scheduler) {
        var result = timerWithMultipleServices.attach(service1);
        result = timerWithMultipleServices.attach(service2);
        result =  timerWithMultipleServices.start();
        runtime:sleep(5);
        result = timerWithMultipleServices.stop();
        test:assertTrue(firstTimerServiceTriggered, msg = "Expected value mismatched");
        test:assertTrue(secondTimerServiceTriggered, msg = "Expected value mismatched");
    } else {
        test:assertFail("Test failed due to an error in the creating of the scheduler.");
    }
}

boolean fourthTimerServiceTriggered = false;
int fourthTimerServiceTriggeredCount = 0;

service object {} service4 = service object {
    remote function onTrigger() {
        fourthTimerServiceTriggeredCount = fourthTimerServiceTriggeredCount + 1;
        if (fourthTimerServiceTriggeredCount > 3) {
            fourthTimerServiceTriggered = true;
        }
    }
};

@test:Config {}
function testTaskTimerWithSameServices() {
    Scheduler|SchedulerError timerWithMultipleServices = new ({intervalInMillis: 1000});
    if (timerWithMultipleServices is Scheduler) {
        var result = timerWithMultipleServices.attach(service4);
        result = timerWithMultipleServices.start();
        runtime:sleep(1.5);
        result = timerWithMultipleServices.attach(service4);
        runtime:sleep(2.7);
        result = timerWithMultipleServices.stop();
        test:assertTrue(fourthTimerServiceTriggered, msg = "Expected value mismatched");
    } else {
        test:assertFail("Test failed due to an error in the creating of the scheduler.");
    }
}
