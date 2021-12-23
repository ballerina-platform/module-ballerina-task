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

import ballerina/lang.runtime;
import ballerina/test;
import ballerina/time;

int count = 1;

class Job1 {

    *Job;
    int i;

    public function execute() {
        count = self.i;
    }

    isolated function init(int i) {
        self.i = i;
    }
}

@test:Config {
    groups: ["OneTimeJob"]
}
function testOneTimeJob() returns error? {
    time:ZoneOffset zoneOffset = {hours: 0, minutes: 0};
    time:Utc currentUtc = time:utcNow();
    time:Utc newTime = time:utcAddSeconds(currentUtc, 3);
    time:Civil time = time:utcToCivil(newTime);
    time.utcOffset = zoneOffset;

    JobId id = check scheduleOneTimeJob(new Job1(5), time);
    runtime:sleep(15);
    test:assertEquals(count, 5, msg = "Expected count mismatched.");
}

int count2 = 0;

class Job2 {

    *Job;
    int i = 1;

    public function execute() {
        count2 = count2 +1;
    }

    isolated function init(int i) {
        self.i = i;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testIntervalJob() returns error? {
    time:Utc currentUtc = time:utcNow();
    time:Utc newTime1 = time:utcAddSeconds(currentUtc, 10);
    time:Utc newTime2 = time:utcAddSeconds(currentUtc, 20);
    time:Civil startTime = time:utcToCivil(newTime1);
    time:Civil endTime = time:utcToCivil(newTime2);

    JobId result = check scheduleJobRecurByFrequency(new Job2(1), 1, startTime = startTime, endTime = endTime);
    runtime:sleep(15);
    test:assertTrue(count2 > 1, msg = "Expected count mismatched.");
}

int count3 = 0;

class Job3 {

    *Job;

    public function execute() {
        count3 = count3 +1;
    }
}

int count4 = 0;

class Job4 {

    *Job;

    public function execute() {
        count4 = count4 +1;
    }
}
@test:Config {
    groups: ["WorkerPool"]
}
function testConfigureWorkerPool() returns error? {
    JobId result = check scheduleJobRecurByFrequency(new Job3(), 1);
    JobId result1 = check scheduleJobRecurByFrequency(new Job4(), 1);
    runtime:sleep(5);
    test:assertTrue(count3 < 7, msg = "Expected count mismatched.");
    test:assertTrue(count4 < 7, msg = "Expected count mismatched.");
    check configureWorkerPool(6, 7000);
    runtime:sleep(5);
    test:assertTrue(count3 > 7, msg = "Expected count mismatched.");
    test:assertTrue(count4 > 7, msg = "Expected count mismatched.");
}

int count5 = 0;

class Job5 {

    *Job;

    public function execute() {
        count5 = count5 +1 ;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testIntervalJobWithCount() returns error? {
    JobId result = check scheduleJobRecurByFrequency(new Job5(), 1, maxCount = 5);
    runtime:sleep(10);
    test:assertEquals(count5, 5, msg = "Expected count mismatched.");
}

int count6 = 0;

class Job6 {

    *Job;

    public function execute() {
        count6 = count6 + 1 ;
    }
}

@test:Config {
    groups: ["FrequencyJob"],
    dependsOn: [testLogIgnore]
}
function testIgnoreTrigger() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job6(), 5, maxCount = 10, taskPolicy = { waitingPolicy: IGNORE });
    runtime:sleep(3);
    check pauseJob(id);
    runtime:sleep(10);
    check resumeJob(id);
    runtime:sleep(8);
    test:assertEquals(count6, 3, msg = "Expected count mismatched.");
}

int count7 = 0;

class Job7 {

    *Job;

    public function execute() {
        count7 = count7 +1 ;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testWaitInWaitingPolicy() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job7(), 5, maxCount = 10, taskPolicy = { waitingPolicy: WAIT });
    runtime:sleep(3);
    check pauseJob(id);
    runtime:sleep(10);
    check resumeJob(id);
    runtime:sleep(15);
    test:assertEquals(count7, 6, msg = "Expected count mismatched.");
}

int count8 = 0;

class Job8 {

    *Job;

    public function execute() {
        count8 = count8 +1 ;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testLogIgnore() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job8(), 5, maxCount = 3, taskPolicy = { waitingPolicy: LOG_AND_IGNORE });
    runtime:sleep(3);
    check pauseJob(id);
    runtime:sleep(10);
    check resumeJob(id);
    runtime:sleep(8);
    test:assertEquals(count8, 3, msg = "Expected count mismatched.");
}

int count9 = 0;

class Job9 {

    *Job;

    public function execute() {
        count9 = count9 + 1;
        panic error ("ERROR: Error occurred during execute the job.");
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testLogAndTerminate() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job9(), 5, maxCount = 2,
    taskPolicy = { errorPolicy: LOG_AND_TERMINATE });
    runtime:sleep(10);
    test:assertEquals(count9, 1, msg = "Expected count mismatched.");
}

int count10 = 0;

class Job10 {

    *Job;

    public function execute() {
        count10 = count10 + 1;
        panic error ("ERROR: Error occurred during execute the job.");
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testTerminate() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job10(), 5, maxCount = 2, taskPolicy = { errorPolicy: TERMINATE });
    runtime:sleep(10);
    test:assertEquals(count9, 1, msg = "Expected count mismatched.");
}

int count11 = 0;

class Job11 {

    *Job;

    public function execute() {
        count11 = count11 + 1;
        panic error ("ERROR: Error occurred during execute the job.");
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testLogAndContinue() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job11(), 5, maxCount = 3,
    taskPolicy = { errorPolicy: LOG_AND_CONTINUE });
    runtime:sleep(12);
    test:assertEquals(count11, 3, msg = "Expected count mismatched.");
}

int count12 = 0;

class Job12 {

    *Job;

    public function execute() {
        count12 = count12 + 1;
        panic error ("ERROR: Error occurred during execute the job.");
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testContinue() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job12(), 5, maxCount = 3, taskPolicy = { errorPolicy: CONTINUE });
    runtime:sleep(12);
    test:assertEquals(count12, 3, msg = "Expected count mismatched.");
}

int count13 = 0;

class Job13 {

    *Job;

    public function execute() {
        count13 = count13 + 1;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testPauseAndResume() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job13(), 5, maxCount = 5);
    runtime:sleep(3);
    check pauseJob(id);
    runtime:sleep(10);
    test:assertEquals(count13, 1, msg = "Expected count mismatched.");
    check resumeJob(id);
    runtime:sleep(8);
    test:assertEquals(count13, 5, msg = "Expected count mismatched.");
}

int count14 = 0;

class Job14 {

    *Job;

    public function execute() {
        count14 = count14 + 1;
    }
}

int count15 = 0;

class Job15 {

    *Job;

    public function execute() {
        count15 = count15 + 1;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testPauseAllJobsAndResumeAllJob() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job15(), 5, maxCount = 5);
    id = check scheduleJobRecurByFrequency(new Job14(), 5, maxCount = 5);
    runtime:sleep(3);
    check pauseAllJobs();
    runtime:sleep(10);
    test:assertEquals(count14, 1, msg = "Expected count mismatched.");
    test:assertEquals(count15, 1, msg = "Expected count mismatched.");
    check resumeAllJobs();
    runtime:sleep(8);
    test:assertEquals(count14, 5, msg = "Expected count mismatched.");
    test:assertEquals(count15, 5, msg = "Expected count mismatched.");
}

int count16 = 0;

class Job16 {

    *Job;

    public function execute() {
        count16 = count16 + 1;
    }
}

int count17 = 0;

class Job17 {

    *Job;

    public function execute() {
        count17 = count17 + 1;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
isolated function testGetRunningJobs() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job16(), 5);
    id = check scheduleJobRecurByFrequency(new Job17(), 5);
    runtime:sleep(3);
    JobId[] ids = getRunningJobs();
    test:assertTrue(ids.length() >= 2, msg = "Expected count mismatched.");
}

int count18 = 0;

class Job18 {

    *Job;

    public function execute() {
        count18 = count18 + 1;
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
function testUnscheduleJob() returns error? {
    JobId id = check scheduleJobRecurByFrequency(new Job18(), 2);
    runtime:sleep(5);
    test:assertEquals(count18, 3, msg = "Expected count mismatched.");
    check unscheduleJob(id);
    runtime:sleep(4);
    test:assertEquals(count18, 3, msg = "Expected count mismatched.");
}


class Job19 {

    *Job;

    public isolated function execute() {
    }
}

@test:Config {
    groups: ["FrequencyJob"]
}
isolated function testCivilRecordValidation() {
    time:Utc currentUtc = time:utcNow();
    time:Civil time = time:utcToCivil(currentUtc);
    time:ZoneOffset zoneOffset = {hours: 30, minutes: 0};
    time.utcOffset = zoneOffset;
    JobId|Error result = scheduleOneTimeJob(new Job19(), time);
    if(result is Error) {
        test:assertTrue(result.message().includes("Couldn't convert given time to milli seconds"),
                    msg = "Output mismatched.");
    } else {
        test:assertFail("Test failed.");
    }
}

int count20 = 0;

class Job20 {

    *Job;

    public function execute() {
        count20 = count20 +1;
    }
}

@test:Config {
    groups: ["WorkerPool"],
    dependsOn: [testIgnoreTrigger]
}
function testConfigureWorker() returns error? {
    check configureWorkerPool(6, 7000);
    JobId result = check scheduleJobRecurByFrequency(new Job20(), 1);
    runtime:sleep(5);
    test:assertTrue((4 < count20 && count20 <= 6), msg = "Expected count mismatched.");
}

int count21 = 0;

class Job21 {

    *Job;
    int i = 1;

    public function execute() {
        count21 = count21 +1;
    }

    isolated function init(int i) {
        self.i = i;
    }
}

@test:Config {
    groups: ["FrequencyJob", "startTime"]
}
function testIntervalJobWithStattTime() returns error? {
    time:Utc currentUtc = time:utcNow();
    time:Utc newTime = time:utcAddSeconds(currentUtc, 10);
    time:Civil startTime = time:utcToCivil(newTime);

    JobId result = check scheduleJobRecurByFrequency(new Job21(1), 1, startTime = startTime);
    runtime:sleep(20);
    test:assertTrue(count21 > 1, msg = "Expected count mismatched.");
}

int count22 = 0;

class Job22 {

    *Job;
    int i = 1;

    public function execute() {
        count22 = count22 +1;
    }

    isolated function init(int i) {
        self.i = i;
    }
}

@test:Config {
    groups: ["FrequencyJob", "endTime"]
}
function testIntervalJobWithEndTime() returns error? {
    time:Utc currentUtc = time:utcNow();
    time:Utc newTime = time:utcAddSeconds(currentUtc, 10);
    time:Civil endTime = time:utcToCivil(newTime);

    JobId result = check scheduleJobRecurByFrequency(new Job22(1), 1, endTime = endTime);
    runtime:sleep(7);
    test:assertTrue(count22 >= 4, msg = "Expected count mismatched.");
}

class Job23 {

    *Job;

    public isolated function execute() {
    }
}

@test:Config {
    groups: ["FrequencyJob", "negative"]
}
isolated function testConfigurationValidation() returns error? {
    JobId result = check scheduleJobRecurByFrequency(new Job23(), 1);
    Error? output = configureWorkerPool(-6, 7000);
    if(output is Error) {
        test:assertTrue(output.message().includes("Cannot create the Scheduler.Thread count must be > 0"));
    } else {
        test:assertFail("Test failed.");
    }
}

@test:Config {
    groups: ["FrequencyJob", "negative"]
}
isolated function testMaxCountValidation() {
    JobId|Error output = scheduleJobRecurByFrequency(new Job23(), 1, maxCount = -4);
    if(output is Error) {
        test:assertTrue(output.message().includes("The maxCount should be a positive integer."));
    } else {
        test:assertFail("Test failed.");
    }
}

@test:Config {
    groups: ["FrequencyJob"],
    dependsOn: [testLogIgnore]
}
isolated function testEmptyRunningJobs() returns error? {
    JobId[] ids = getRunningJobs();
    if (ids.length() > 0) {
        foreach JobId i in ids {
           check unscheduleJob(i);
        }
        ids = getRunningJobs();
        test:assertTrue(ids.length() == 0);
    } else {
        test:assertTrue(ids.length() == 0);
    }
    Error? output = configureWorkerPool(7, 7000);
    JobId result = check scheduleJobRecurByFrequency(new Job23(), 1);
    ids = getRunningJobs();
    test:assertTrue(ids.length() == 1);
}

@test:Config {
    groups: ["FrequencyJob", "negative"]
}
isolated function testUnscheduleJobs() returns error? {
    JobId id = {id: 12};
    Error? result = unscheduleJob(id);
    if(result is Error) {
        test:assertTrue(result.message().includes("Invalid job id"));
    } else {
        test:assertFail("Test failed.");
    }
}

@test:Config {
    groups: ["FrequencyJob", "negative"]
}
isolated function testScheduleJobsWithNegativeInterval() returns error? {
    JobId|Error output = scheduleJobRecurByFrequency(new Job23(), -1d);
    if(output is Error) {
        test:assertTrue(output.message().includes("Repeat interval must be >= 0"));
    } else {
        test:assertFail("Test failed.");
    }
}

@test:Config {
    groups: ["FrequencyJob", "negative"]
}
isolated function testScheduleJobsWithInvalidStartTime() returns error? {
    time:Civil startTime = {"utcOffset":{"hours":0,"minutes":0},"timeAbbrev":"Z","dayOfWeek":1,"year":2021,"month":12,
                            "day":13,"hour":7,"minute":25,"second":40.893987};
    JobId|Error output = scheduleJobRecurByFrequency(new Job23(), 4, startTime = startTime);
    if output is Error {
        test:assertTrue(output.message().includes("Invalid time"), output.message());
    } else {
        test:assertFail("Test failed.");
    }
}
