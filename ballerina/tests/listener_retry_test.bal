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

isolated int[] retryResults = [];
isolated int[] singleRetryResults = [];

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

listener Listener retryExponentialListener = new (trigger = {
    interval: 20,
    maxCount: 1,
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: EXPONENTIAL, 
        retryInterval: 2, 
        maxInterval: 50
    }
});

listener Listener retryExceedingIntervalListener = new (trigger = {
    interval: 300,
    maxCount: 1,
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: EXPONENTIAL, 
        retryInterval: 21, 
        maxInterval: 20
    }
});

listener Listener retryExceedingRetryIntervalListener = new (trigger = {
    interval: 20,
    maxCount: 1,
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: EXPONENTIAL, 
        retryInterval: 21, 
        maxInterval: 20
    }
});

listener Listener retryFixedListener = new (trigger = {
    interval: 6,
    maxCount: 2,
    taskPolicy: {
        errorPolicy: CONTINUE
    },
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: FIXED, 
        retryInterval: 1, 
        maxInterval: 6
    }
});

listener Listener retryExponentialRecurringListener = new (trigger = {
    interval: 6,
    maxCount: 2,
    taskPolicy: {
        errorPolicy: CONTINUE
    },
    retryConfig: {
        maxAttempts: 5, 
        backoffStrategy: EXPONENTIAL, 
        retryInterval: 1, 
        maxInterval: 6
    }
});

Service singleEventServiceWithErrors = service object {
    isolated function execute() returns error? {
        lock {
            singleRetryResults.push(singleRetryResults.length() + 1);
            return error("STANDARD_ERROR");
        }
    }
};

Service retryServiceWithErrors = service object {
    isolated function execute() returns error? {
        lock {
            retryResults.push(retryResults.length() + 1);
            return error("STANDARD_ERROR");
        }
    }
};

Service retryServiceWithIntermittentErrors = service object {
    isolated function execute() returns error? {
        lock {
            if retryResults.length() == 0 {
                retryResults.push(retryResults.length() + 1);
                return error("STANDARD_ERROR");
            }
            retryResults.push(retryResults.length() + 1);
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

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithListeners() returns error? {
    check retryOneTimeListener.attach(singleEventServiceWithErrors);
    check retryOneTimeListener.'start();
    runtime:registerListener(retryOneTimeListener);
    runtime:sleep(5);
    // No retries will occur because the trigger interval and retry interval are both set to 1 second.
    int expectedCount = 1;
    lock {
        test:assertEquals(singleRetryResults.length(), expectedCount);
        singleRetryResults = [];
    }
    check retryOneTimeListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithExponentialInterval() returns error? {
    check retryExponentialListener.attach(retryServiceWithErrors);
    check retryExponentialListener.'start();
    runtime:registerListener(retryExponentialListener);
    runtime:sleep(35);
    // The retry intervals are 2, 4, 8, and 16 seconds.
    // 2 + 4 + 8 = 14 seconds
    // next retry will be 16 seconds, which exceeds the max interval of 20 seconds
    // Expected count : 4 (1 initial + 3 retries (2, 4, 8 seconds))
    int expectedCount = 4;
    lock {
        test:assertEquals(retryResults.length(), expectedCount);
        retryResults = [];
    }
    check retryExponentialListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithExceedingMaxInterval() returns error? {
    check retryExceedingIntervalListener.attach(periodicEventServiceWithErrors);
    check retryExceedingIntervalListener.'start();
    runtime:registerListener(retryExceedingIntervalListener);
    runtime:sleep(5);
    // No retries will occur because the retry interval is greater than the max interval.
    int expectedCount = 1;
    lock {
        test:assertEquals(eventResults.length(), expectedCount);
        eventResults = [];
    }
    check retryExceedingIntervalListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithExceedingRetryInterval() returns error? {
    check retryExceedingRetryIntervalListener.attach(periodicEventServiceWithErrors);
    check retryExceedingRetryIntervalListener.'start();
    runtime:registerListener(retryExceedingRetryIntervalListener);
    runtime:sleep(5);
    // No retries will occur because the retry interval is greater than the max interval.
    int expectedCount = 1;
    lock {
        test:assertEquals(eventResults.length(), expectedCount);
        eventResults = [];
    }
    check retryExceedingIntervalListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithRecurringConfigs() returns error? {
    check retryListener.attach(periodicEventServiceWithErrors);
    check retryListener.'start();
    runtime:registerListener(retryListener);
    runtime:sleep(15);
    // 1 initial + 5 retries
    int expectedCount = 6;
    lock {
        test:assertEquals(eventResults.length(), expectedCount);
        eventResults = [];
    }
    check retryListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithRecurrings() returns error? {
    check retryFixedListener.attach(periodicEventServiceWithErrors);
    check retryFixedListener.'start();
    runtime:registerListener(retryFixedListener);
    runtime:sleep(30);
    // 2 times x (1 initial + 5 retries) = 12
    int expectedCount = 12;
    lock {
        test:assertEquals(eventResults.length(), expectedCount);
        eventResults = [];
    }
    check retryFixedListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithRecurringsAndExponentialIntervals() returns error? {
    check retryExponentialRecurringListener.attach(periodicEventServiceWithErrors);
    check retryExponentialRecurringListener.'start();
    runtime:registerListener(retryExponentialRecurringListener);
    runtime:sleep(15);
    // 2 x (1 initial + 2 retries) = 6
    int expectedCount = 6;
    lock {
        test:assertEquals(eventResults.length(), expectedCount);
        eventResults = [];
    }
    check retryExponentialRecurringListener.gracefulStop();
}

@test:Config {
    groups: ["listener", "retry"]
}
function testRetryWithIntermittentErrors() returns error? {
    check retryFixedListener.attach(retryServiceWithIntermittentErrors);
    check retryFixedListener.'start();
    runtime:registerListener(retryFixedListener);
    runtime:sleep(10);
    // 2 initial + 1 retry
    int expectedCount = 3;
    lock {
        test:assertEquals(retryResults.length(), expectedCount);
        retryResults = [];
    }
    check retryFixedListener.gracefulStop();
}
