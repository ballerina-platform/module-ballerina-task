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

Listener timerForNoResourceService = new ({
    intervalInMillis: 1
});

@test:Config {}
public isolated function testZeroInterval() {
    Listener|error timer = trap new ({
        intervalInMillis: 0
    });
    test:assertTrue(timer is error);
    if (timer is error) {
        string message = timer.message();
         test:assertEquals(message, "Timer scheduling interval should be a positive integer.");
    }

}

@test:Config {}
public isolated function testNegativeDelay() {
    Listener|error timer = trap new ({
        intervalInMillis: 500,
        initialDelayInMillis: -1000
    });
    test:assertTrue(timer is error);
    if (timer is error) {
         test:assertEquals(timer.message(), "Timer scheduling delay should be a non-negative integer.");
    }
}

@test:Config {}
public isolated function testNegativeInteval() {
    Listener|error timer = trap new ({
        intervalInMillis: -500,
        initialDelayInMillis: 1000
    });
    test:assertTrue(timer is error);
    if (timer is error) {
         test:assertEquals(timer.message(), "Timer scheduling interval should be a positive integer.");
    }
}

@test:Config {}
public isolated function testInvalidCronExpression() {
    CronTriggerConfiguration configuration = {
        cronExpression: "invalid cron expression"
    };
    Listener|error timer = trap new (configuration);
    test:assertTrue(timer is error);
    if (timer is error) {
         test:assertEquals(timer.message(), "Cron Expression \"invalid cron expression\" is invalid.");
    }
}

@test:Config {}
public isolated function testInvalidThreadCount() {
    Scheduler|error timer = trap new ({ threadCount: 0});
    test:assertTrue(timer is error);
    if (timer is error) {
        test:assertEquals(timer.message(), "Thread count must be greater than 0.");
    }
}

@test:Config {}
public isolated function testInvalidThreadPriority() {
    Scheduler|error timer = trap new ({ threadPriority: 11});
    test:assertTrue(timer is error);
    if (timer is error) {
        test:assertEquals(timer.message(), "Thread priority must be an integer value between 1 and 10.");
    }
}

@test:Config {}
public isolated function testInvalidThreadsholdValue() {
    Scheduler|error timer = trap new ({ thresholdInMillis: 0});
    test:assertTrue(timer is error);
    if (timer is error) {
        test:assertEquals(timer.message(), "Misfire threadshold value should be a positive integer.");
    }
}
