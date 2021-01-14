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

TimerConfiguration configuration = {
    intervalInMillis: 1000,
    initialDelayInMillis: 1000
};

int counter1 = 0;
int counter2 = 0;

service object {} pauseResumeTimerService1 = service object {
    remote function onTrigger() {
        counter1 = counter1 + 1;
    }
};

service object{} pauseResumeTimerService2 = service object {
    remote function onTrigger() {
        counter2 = counter2 + 1;
    }
};

@test:Config {}
function testTaskPauseAndResume() {
    Scheduler timer1 = new (configuration);
    Scheduler timer2 = new (configuration);
    checkpanic timer1.attach(pauseResumeTimerService1);
    checkpanic timer2.attach(pauseResumeTimerService2);
    checkpanic timer1.start();
    checkpanic timer2.start();
    runtime:sleep(3.5);
    var result = timer1.pause();
    if (result is error) {
        test:assertFail("An error occurred when pausing the scheduler");
    }
    runtime:sleep(4);
    test:assertEquals(counter1, 3, msg = "Values are mismatched");
    test:assertTrue(counter2 - counter1 >= 3, msg = "Test failed");
    result = timer1.resume();
    runtime:sleep(1);
    if (result is error) {
        return;
    }
    checkpanic timer1.stop();
    checkpanic timer2.stop();
    test:assertTrue(counter1 > 3, msg = "Expected value mismatched");
}
