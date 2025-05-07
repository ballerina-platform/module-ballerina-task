// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
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

import ballerina/time;
import ballerina/test;
import ballerina/lang.runtime;

isolated int[] oneTimeResults = [];
isolated int[] recurringResults = [];

listener Listener oneTimeListener = new(schedule = {
    triggerTime: time:utcToCivil(time:utcAddSeconds(time:utcNow(), 3))
});

listener Listener recurringListener = new(schedule = {
    interval: 2,
    startTime: time:utcToCivil(time:utcAddSeconds(time:utcNow(), 3)),
    maxCount: 4
});

Service oneTimeService = service object {
    function onTrigger() {
        lock {
            oneTimeResults.push(1);
        }
    }
};

Service recurringService = service object {
    function onTrigger() {
        lock {
            recurringResults.push(recurringResults.length() + 1);
        }
    }
};

@test:Config {
    groups: ["listener"]
}
function testOneTimeTask() returns error? {
    check oneTimeListener.attach(oneTimeService);
    check oneTimeListener.'start();
    runtime:registerListener(oneTimeListener);
    runtime:sleep(10);
    lock {
        test:assertEquals(oneTimeResults.length(), 1);
    }
}

@test:Config {
    groups: ["listener"]
}
function testRecurringTask() returns error? {
    check recurringListener.attach(recurringService);
    check recurringListener.'start();
    runtime:registerListener(recurringListener);
    runtime:sleep(15);
    lock {
        test:assertEquals(recurringResults.length(), 4);
    }
}

