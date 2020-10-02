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
import ballerina/runtime;

// Service for the appointment task
AppointmentData appointmentData = {
    seconds: "0/2",
    minutes: "*",
    hours: "*",
    daysOfMonth: "*",
    months: "*",
    daysOfWeek: "?",
    year: "*"
};

AppointmentConfiguration appointmentConfiguration = {
    appointmentDetails: appointmentData
};

int count = 0;

listener Listener appointment = new({triggerConfig: appointmentConfiguration});

function getCount() returns int {
    return count;
}

service appointmentService on appointment {
    resource function onTrigger() {
        count = count + 1;
    }
}

// Service for the timer task
TimerConfiguration timerConfig = {
    intervalInMillis: 2000,
    initialDelayInMillis: 1000
};

int timerCount = 0;

listener Listener timer = new({triggerConfig: timerConfig});

function getTimerCount() returns int {
    return timerCount;

}

service timerService on timer {
    resource function onTrigger() {
        timerCount = timerCount + 1;
    }
}

// Service for the timer task with an inline config
listener Listener inLineTimer = new({triggerConfig: { intervalInMillis: 2000, initialDelayInMillis: 1000 }});

int inLineTimerConfigCount = 0;

function getInLineTimerCount() returns int {
    return inLineTimerConfigCount;
}

service inLineTimerService on inLineTimer {
    resource function onTrigger() {
        inLineTimerConfigCount = inLineTimerConfigCount + 1;
    }
}

// Service for the timer task with a limited no Of runs
TimerConfiguration timerConfigForWithLimitedNumberOfRuns = {
    intervalInMillis: 1000,
    noOfRecurrences: 3
};

int countForWithLimitedNumberOfRuns = 0;

listener Listener timerForWithLimitedNumberOfRuns = new({triggerConfig: timerConfigForWithLimitedNumberOfRuns});

function getCountForWithLimitedNumberOfRuns() returns int {
    return countForWithLimitedNumberOfRuns;
}

service timerServiceForWithLimitedNumberOfRuns on timerForWithLimitedNumberOfRuns {
    resource function onTrigger() {
        countForWithLimitedNumberOfRuns = countForWithLimitedNumberOfRuns + 1;
    }
}

// Service for the timer task with out delay
TimerConfiguration configWithOutDelay = {
    intervalInMillis: 2000
};

int countForConfigWithOutDelay = 0;

listener Listener timerForConfigWithOutDelay = new({triggerConfig: configWithOutDelay});

function getCountForConfigWithOutDelay() returns int {
    return countForConfigWithOutDelay;
}

service timerServiceForConfigWithOutDelay on timerForConfigWithOutDelay {
    resource function onTrigger() {
        countForConfigWithOutDelay = countForConfigWithOutDelay + 1;
    }
}

@test:Config {
}
public function testAppointmentAndTimerConfig() {
    runtime:sleep(4000);
    test:assertEquals(getCountForWithLimitedNumberOfRuns(), 3);
    test:assertTrue(getCount() > 0);
    test:assertTrue(getTimerCount() > 0);
    test:assertTrue(getInLineTimerCount() > 0);
    test:assertTrue(getCountForConfigWithOutDelay() > 0);
}
