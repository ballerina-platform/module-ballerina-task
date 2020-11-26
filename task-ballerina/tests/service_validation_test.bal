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
import ballerina/stringutils;

service noResourceService = service {};

listener Listener testListner = new({intervalInMillis: 1000});

@test:Config {}
public function testForNoResourceService() {
    var attachResult = trap testListner.__attach(noResourceService);
    checkpanic testListner.__start();
    test:assertTrue(attachResult is error);
    if (attachResult is error) {
        test:assertTrue(stringutils:contains(attachResult.message(), "Invalid number of resources found in service"));

    }
}

service moreThanOneResourceService = service {
    resource isolated function onTrigger() {}

    resource isolated function onError(error e) {}
};

@test:Config {}
public function testForMoreThanOneResource() {
    var attachResult = trap testListner.__attach(moreThanOneResourceService);
    checkpanic testListner.__start();
    test:assertTrue(attachResult is error);
    if (attachResult is error) {
         test:assertTrue(stringutils:contains(attachResult.message(), "Invalid number of resources found in service"));
    }
}
