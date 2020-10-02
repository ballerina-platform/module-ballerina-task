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

string name = "John";
int age = 0;
int acNumber = 100;
float balance = 0.00;
string output = "";

public type Account record {
    int number;
    float balance;
};

public type Person record {
    string name;
    int age;
};

service multipleAttachService = service {
    resource function onTrigger(Person p, Account a) {
        name = <@untainted>p.name;
        age = <@untainted>p.age;
        acNumber = <@untainted>a.number;
        balance = <@untainted>a.balance;
        output = "Name: " + name + " Age: " + age.toString() + " A/C: " + acNumber.toString() + " Balance: " +
        balance.toString();
    }
};

@test:Config {}
function multipleAttachmentTest() {
    Person sam = {name: "Sam", age: 29};
    Account acc = {number: 150590, balance: 11.35};
    TimerConfiguration timerConfiguration = { intervalInMillis: 1000 };
    Scheduler multipleAttachmentTimer = new ({ triggerConfig: timerConfiguration });
    var attachResult = multipleAttachmentTimer.attach(multipleAttachService, sam, acc);
    var startResult = multipleAttachmentTimer.start();
    runtime:sleep(4000);
    checkpanic multipleAttachmentTimer.stop();
    test:assertEquals(output, "Name: Sam Age: 29 A/C: 150590 Balance: 11.35",
            msg = "Response payload mismatched");
}
