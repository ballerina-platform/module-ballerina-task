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

import ballerina/test;

TimerConfiguration config =  {intervalInMillis: 1};
Scheduler timerForNoResourceService = new ({triggerConfig: config});

@test:Config {}
public function testZeroInterval() {
    Scheduler|error timer = trap new ({triggerConfig: {
        intervalInMillis: 0
    }});
    test:assertTrue(timer is error);
    test:assertEquals(timer.toString(), "error(\"Timer scheduling interval should be a positive integer.\")");
}

@test:Config {}
public function testNegativeDelay() {
    Scheduler|error timer = trap new ({ triggerConfig: {
        intervalInMillis: 500,
        initialDelayInMillis: -1000
    }});
    test:assertTrue(timer is error);
    test:assertEquals(timer.toString(), "error(\"Timer scheduling delay should be a non-negative value.\")");
}

@test:Config {}
public function testNegativeInteval() {
    Scheduler|error timer = trap new ({ triggerConfig: {
        intervalInMillis: -500,
        initialDelayInMillis: 1000
    }});
    test:assertTrue(timer is error);
    test:assertEquals(timer.toString(), "error(\"Timer scheduling interval should be a positive integer.\")");
}

@test:Config {}
public function testInvalidAppointmentData() {
    AppointmentData appointmentData = {
        seconds: "invalid",
        minutes: " ",
        hours: "Appointment",
        daysOfMonth: " ",
        months: "Data",
        daysOfWeek: "*",
        year: "*"
    };

    AppointmentConfiguration configuration = {
        appointmentDetails: appointmentData
    };

    Scheduler|error timer = trap new ({triggerConfig: configuration});
    test:assertTrue(timer is error);
    test:assertEquals(timer.toString(), "error(\"AppointmentData \"{\"seconds\":\"invalid\",\"minutes\":\" \"," +
        "\"hours\":\"Appointment\",\"daysOfMonth\":\" \",\"months\":\"Data\",\"daysOfWeek\":\"*\",\"year\":\"*\"}\" " +
        "is invalid.\")");
}

type DuplicateAppointmentData record {
    string seconds?;
    string minutes?;
    string hours?;
    string daysOfMonth?;
    string months?;
    string daysOfWeek?;
    string year?;
};

@test:Config {
}
public function testInvalidAppointmentDataRecord() {

    DuplicateAppointmentData appointmentData = {
        seconds: "0/2",
        minutes: "*",
        hours: "*",
        daysOfMonth: "*",
        months: "*",
        daysOfWeek: "?",
        year: "*"
    };
    test:assertFalse(appointmentData is AppointmentData);
}

@test:Config {}
public function testInvalidCronExpression() {
    AppointmentConfiguration configuration = {
        appointmentDetails: "invalid cron expression"
    };
    Scheduler|error timer = trap new ({triggerConfig: configuration});
    test:assertTrue(timer is error);
    test:assertEquals(timer.toString(), "error(\"Cron Expression \"invalid cron expression\" is invalid.\")");
}
