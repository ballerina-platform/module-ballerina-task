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

isolated int[] oneTimeEventResults = [];
isolated int[] recurringEventResults = [];
isolated int[] multiServiceEventCounts = [];
isolated int[] taskExecutionCounts = [];
isolated int[] errorResult = [];
isolated int[] eventResults = [];

listener Listener singleListener = new (trigger = {
    interval: 1,
    maxCount: 1
});

listener Listener singleEventListener = new (trigger = {
    interval: 1,
    maxCount: 1
});

listener Listener periodicEventListener = new (trigger = {
    interval: 2,
    startTime: time:utcToCivil(time:utcAddSeconds(time:utcNow(), 3)),
    maxCount: 4
});

Service firstConcurrentService = service object {
    isolated function execute() {
        lock {
            multiServiceEventCounts.push(1);
        }
    }
};

Service secondConcurrentService = service object {
    isolated function execute() {
        lock {
            multiServiceEventCounts.push(1);
        }
    }
};

Service taskExecutionService = service object {
    isolated function execute() {
        lock {
            taskExecutionCounts.push(1);
        }
    }
};

Service singleEventService = service object {
    isolated function execute() {
        lock {
            oneTimeEventResults.push(1);
        }
    }
};

Service errorService = service object {
    isolated function execute() returns error? {
        lock {
            errorResult.push(1);
        }
        return error("Error occurred in the service");
    }
};

Service periodicEventService = service object {
    isolated function execute() returns error? {
        lock {
            recurringEventResults.push(recurringEventResults.length() + 1);
        }
    }
};

Service periodicEventServiceWithErrors = service object {
    isolated function execute() returns error? {
        lock {
            eventResults.push(eventResults.length() + 1);
            return error("STANDARD_ERROR");
        }
    }
};

listener Listener retryOneTimeListener = new (trigger = {
    interval: 1,
    maxCount: 1,
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: EXPONENTIAL, 
        retryInterval: 1, 
        maxInterval: 20
    }
});

listener Listener retryListener = new (trigger = {
    interval: 25,
    maxCount: 2,
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: FIXED, 
        retryInterval: 1, 
        maxInterval: 20
    }
});

listener Listener retryListenerWithShortInterval = new (trigger = {
    interval: 1,
    maxCount: 2,
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: EXPONENTIAL, 
        retryInterval: 2, 
        maxInterval: 20
    }
});

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
    check periodicEventListener.attach(errorService);
    check periodicEventListener.'start();
    runtime:registerListener(periodicEventListener);
    runtime:sleep(3);
    lock {
        test:assertEquals(errorResult.length(), 1);
    }
    check periodicEventListener.gracefulStop();
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
function testDetachListener() returns error? {
    lock {
        multiServiceEventCounts = [];
    }
    check singleEventListener.attach(firstConcurrentService);
    check singleEventListener.attach(secondConcurrentService);
    check singleEventListener.detach(firstConcurrentService);
    check singleEventListener.'start();
    runtime:registerListener(singleEventListener);
    runtime:sleep(10);
    lock {
        test:assertEquals(multiServiceEventCounts.length(), 1);
    }
    check singleEventListener.gracefulStop();
}

@test:Config {
    groups: ["listener"]
}
function testDetachInvalidListener() returns error? {
    lock {
        multiServiceEventCounts = [];
    }
    check singleEventListener.gracefulStop();
    Error? result = singleEventListener.detach(firstConcurrentService);
    test:assertTrue(result is Error);
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

@test:Config {
    groups: ["listener"]
}
function testImmediateStop() returns error? {
    lock {
        multiServiceEventCounts = [];
    }
    check singleListener.attach(firstConcurrentService);
    check singleListener.attach(secondConcurrentService);
    check singleListener.'start();
    runtime:registerListener(singleListener);
    check singleListener.immediateStop();
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

@test:Config {
    groups: ["listener"]
}
function testConfigureWorkerPoolWithListeners() returns error? {
    lock {
        multiServiceEventCounts = [];
    }
    check singleListener.attach(firstConcurrentService);
    check singleListener.'start();
    runtime:registerListener(singleListener);
    Error? result = configureWorkerPool(10, 100);
    test:assertTrue(result !is Error);
    check singleListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryConfigurationsWithListeners() returns error? {
    check retryOneTimeListener.attach(periodicEventServiceWithErrors);
    check retryOneTimeListener.'start();
    runtime:registerListener(retryOneTimeListener);
    runtime:sleep(5);
    lock {
        test:assertEquals(eventResults.length(), 1);
        eventResults = [];
    }
    check retryOneTimeListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryConfigurationsWithListenersMultipleTimes() returns error? {
    check retryListener.attach(periodicEventServiceWithErrors);
    check retryListener.'start();
    runtime:registerListener(retryListener);
    runtime:sleep(15);
    lock {
        test:assertEquals(eventResults.length(), 6);
        eventResults = [];
    }
    check retryListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryConfigurationsWithListenersWithShortInterval() returns error? {
    check retryListenerWithShortInterval.attach(periodicEventServiceWithErrors);
    check retryListenerWithShortInterval.'start();
    runtime:registerListener(retryListenerWithShortInterval);
    runtime:sleep(5);
    lock {
        test:assertEquals(eventResults.length(), 1);
        eventResults = [];
    }
    check retryListenerWithShortInterval.gracefulStop();
}
