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

int triggeredCount1 = 0;

service object{} misfireService1 = service object {
    resource function get onTrigger() {
        triggeredCount1 = triggeredCount1 + 1;
    }
};

@test:Config {}
function testFireNowWithService1() returns error? {
    Scheduler taskTimer = new ({ intervalInMillis: 3000, noOfRecurrences: 1, misfirePolicy: "fireNow"});
    check taskTimer.attach(misfireService1);
    check taskTimer.start();
    check taskTimer.pause();
    runtime:sleep(5000);
    test:assertEquals(triggeredCount1, 0, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resume();
    runtime:sleep(2000);
    check taskTimer.stop();
    test:assertEquals(triggeredCount1, 1, msg = "Expected count mismatched.");
}

int triggeredCount8 = 0;
service object {} misfireService8 = service object {
    resource function get onTrigger() {
        triggeredCount8 = triggeredCount8 + 1;
    }
};
@test:Config {}
function testIgnoreMisfiresPoilcyWithService8() returns error? {
    Scheduler taskTimer = new ({ intervalInMillis: 3000, noOfRecurrences: 1 , misfirePolicy: "ignorePolicy"});
    check taskTimer.attach(misfireService8);
    check taskTimer.start();
    check taskTimer.pause();
    runtime:sleep(5000);
    test:assertEquals(triggeredCount8, 0, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resume();
    runtime:sleep(2000);
    check taskTimer.stop();
    test:assertEquals(triggeredCount8, 1, msg = "Expected count mismatched.");
}

int triggeredCount9 = 0;

service object {} misfireService9 = service object {
    resource function get onTrigger() {
        triggeredCount9 = triggeredCount9 + 1;
    }
};

@test:Config {}
function testSmartPolicyWithService9() returns error? {
    Scheduler taskTimer = new ({ intervalInMillis: 3000, noOfRecurrences: 1});
    check taskTimer.attach(misfireService9);
    check taskTimer.start();
    check taskTimer.pause();
    runtime:sleep(5000);
    test:assertEquals(triggeredCount9, 0, msg = "Expected count mismatched during the scheduler pause.");
    check taskTimer.resume();
    runtime:sleep(2000);
    check taskTimer.stop();
    test:assertEquals(triggeredCount9, 1, msg = "Expected count mismatched.");
}
