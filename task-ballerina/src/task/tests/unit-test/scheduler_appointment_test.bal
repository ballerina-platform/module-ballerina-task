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

boolean appoinmentFirstTriggered = false;
boolean appoinmentSecondTriggered = false;
int appoinmentTriggerCount1 = 0;
int appoinmentTriggerCount2 = 0;
int appoinmentTriggerCount3 = 0;

service appointmentService1 = service {
    resource function onTrigger() {
        appoinmentTriggerCount1 += 1;
        if (appoinmentTriggerCount1 > 3) {
            appoinmentFirstTriggered = true;
        }
    }
};

service appointmentService2 = service {
    resource function onTrigger() {
        appoinmentTriggerCount2 += 1;
        if (appoinmentTriggerCount2 > 3) {
            appoinmentSecondTriggered = true;
        }
    }
};

service appointmentService3 = service {
    resource function onTrigger() {
        appoinmentTriggerCount3 += 1;
    }
};

@test:Config {}
function testSchedulerWithMultipleServices() {
    string cronExpression = "* * * * * ? *";
    Scheduler appointment = new ({appointmentDetails: cronExpression});
    checkpanic appointment.attach(appointmentService1);
    checkpanic appointment.attach(appointmentService2);
    checkpanic appointment.start();
    runtime:sleep(4000);
    checkpanic appointment.stop();
    test:assertTrue(appoinmentFirstTriggered, msg = "Response payload mismatched");
    test:assertTrue(appoinmentSecondTriggered, msg = "Response payload mismatched");
}

@test:Config {}
function testLimitedNumberOfRuns() {
    string cronExpression = "* * * * * ? *";
    AppointmentConfiguration configuration = {
        appointmentDetails: cronExpression,
        noOfRecurrences: 3
    };
    Scheduler appointmentWithLimitedRuns = new (configuration);
    var result = appointmentWithLimitedRuns.attach(appointmentService3);
    checkpanic appointmentWithLimitedRuns.start();
    runtime:sleep(5000);
    checkpanic appointmentWithLimitedRuns.stop();
    test:assertEquals(appoinmentTriggerCount3, 3, msg = "Expected value mismatched");
}
