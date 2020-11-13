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

service noResourceService = service {};

Person person = {name: "Sam", age: 29};

Scheduler simpleScheduler = new ();

@test:Config {}
public function testForNoResourceService() {
    var attachResult = simpleScheduler.scheduleJob(noResourceService, {intervalInMillis: 1000}, person);
    test:assertTrue(attachResult is error);
    if (attachResult is error) {
         test:assertEquals(attachResult.message(), "Failed to schedule the job in the scheduler");
    }
}

service moreThanOneResourceService = service {
    resource isolated function onTrigger() {}

    resource isolated function onError(error e) {}
};

@test:Config {}
public function testForMoreThanOneResource() {
    var attachResult = simpleScheduler.scheduleJob(moreThanOneResourceService, {intervalInMillis: 1000}, person);
    test:assertTrue(attachResult is error);
    if (attachResult is error) {
         test:assertEquals(attachResult.message(), "Failed to schedule the job in the scheduler");
    }
}
