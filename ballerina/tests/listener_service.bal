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

import ballerina/lang.runtime;
import ballerina/test;
import ballerina/time;
import ballerina/io;

isolated int[] oneTimeEventResults = [];
isolated int[] recurringEventResults = [];
isolated int[] multiServiceEventCounts = [];
isolated int[] taskExecutionCounts = [];
isolated int[] errorCount = [];

listener Listener singleListener = new (schedule = time:utcToCivil(time:utcAddSeconds(time:utcNow(), 8)));
listener Listener singleEventListener = new (schedule = time:utcToCivil(time:utcAddSeconds(time:utcNow(), 3)));

listener Listener periodicEventListener = new (schedule = {
    interval: 2,
    startTime: time:utcToCivil(time:utcAddSeconds(time:utcNow(), 3)),
    maxCount: 4
});

Service firstConcurrentService = service object {
    isolated function onTrigger() {
        lock {
            multiServiceEventCounts.push(1);
        }
    }

    isolated function onError() {
        io:println("Error occurred in the service");
    }
};

Service secondConcurrentService = service object {
    isolated function onTrigger() {
        lock {
            multiServiceEventCounts.push(1);
        }
    }

    isolated function onError() {
        io:println("Error occurred in the service");
    }
};

Service taskExecutionService = service object {
    isolated function onTrigger() {
        lock {
            taskExecutionCounts.push(1);
        }
    }

    isolated function onError() {
        io:println("Error occurred in the service");
    }
};

Service singleEventService = service object {
    isolated function onTrigger() {
        lock {
            oneTimeEventResults.push(1);
        }
    }

    isolated function onError() {
        io:println("Error occurred in the service");
    }
};

Service errorService = service object {
    isolated function onTrigger() {
        panic error("Error occurred in the service");
    }

    isolated function onError() {
        lock {
            errorCount.push(1);
        }
    }
};

Service periodicEventService = service object {
    isolated function onTrigger() {
        lock {
            recurringEventResults.push(recurringEventResults.length() + 1);
        }
    }

    isolated function onError() {
        io:println("Error occurred in the service");
    }
};

@test:Config {
    groups: ["listener"]
}
function testOneTimeTaskWithListener() returns error? {
    check singleEventListener.attach(singleEventService);
    check singleEventListener.'start();
    runtime:registerListener(singleEventListener);
    runtime:sleep(10);
    lock {
        test:assertEquals(oneTimeEventResults.length(), 1);
    }
    check singleEventListener.gracefulStop();
}

@test:Config {
    groups: ["listener"]
}
function testErrorsWithListener() returns error? {
    check singleEventListener.attach(errorService);
    check singleEventListener.'start();
    runtime:registerListener(singleEventListener);
    runtime:sleep(10);
    lock {
        test:assertEquals(errorCount.length(), 1);
    }
}

@test:Config {
    groups: ["listener"]
}
function testRecurringTaskWithListener() returns error? {
    check periodicEventListener.attach(periodicEventService);
    check periodicEventListener.'start();
    runtime:registerListener(periodicEventListener);
    runtime:sleep(15);
    lock {
        test:assertEquals(recurringEventResults.length(), 4);
    }
    check periodicEventListener.gracefulStop();
}

@test:Config {
    groups: ["listener"]
}
function testOneTimeTaskWithMultipleServices() returns error? {
    lock {
        multiServiceEventCounts = [];
    }
    check singleEventListener.attach(firstConcurrentService);
    check singleEventListener.attach(secondConcurrentService);
    check singleEventListener.'start();
    runtime:registerListener(singleEventListener);
    runtime:sleep(10);
    lock {
        test:assertEquals(multiServiceEventCounts.length(), 2);
    }
    check singleEventListener.gracefulStop();
}

@test:Config {
    groups: ["listener"]
}
function testGracefulStop() returns error? {
    lock {
        multiServiceEventCounts = [];
    }
    check singleListener.attach(firstConcurrentService);
    check singleListener.attach(secondConcurrentService);
    check singleListener.'start();
    runtime:registerListener(singleListener);
    check singleListener.gracefulStop();
    runtime:sleep(5);
    int eventCountAfterStop;
    lock {
        eventCountAfterStop = multiServiceEventCounts.length();
    }
    runtime:sleep(5);
    lock {
        test:assertEquals(eventCountAfterStop, multiServiceEventCounts.length());
    }
}
