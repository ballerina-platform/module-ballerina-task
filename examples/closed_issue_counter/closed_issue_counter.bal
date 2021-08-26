// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/http;
import ballerina/task;
import ballerina/time;
import ballerina/jballerina.java;

listener http:Listener issueCounter = new (9092);

service /scheduler on issueCounter {

    resource function post recurJob/[decimal interval](string? startTime) returns error|int {
        if (startTime is string) {
            task:JobId jobId = check task:scheduleJobRecurByFrequency(new Job(), interval,
            startTime = check time:civilFromString(startTime));
            return jobId.id;
        } else {
            task:JobId jobId = check task:scheduleJobRecurByFrequency(new Job(), interval);
            return jobId.id;
        }
    }

    resource function post recurJob/[int repeatingCount]/[decimal interval](string? startTime) returns error|int {
        task:JobId jobId = check task:scheduleJobRecurByFrequency(new Job(), interval, repeatingCount);
        return jobId.id; 
    }

    resource function post oneTimeJob(@http:Payload string time) returns error|int {
        task:JobId jobId = check task:scheduleOneTimeJob(new Job(),  check time:civilFromString(time));
        return jobId.id; 
    }

    resource function post unscheduleJob/[int id]() returns error|string {
        task:JobId jobId = {id: id};
        check task:unscheduleJob(jobId);
        return "Unscheduled job.";   
    }

    resource function post pauseJob/[int id]() returns error|string {
        task:JobId jobId = {id: id};
        check task:pauseJob(jobId);
        return "Paused job.";   
    }

    resource function post resumeJob/[int id]() returns error|string {
        task:JobId jobId = {id: id};
        check task:resumeJob(jobId);
        return "Resumed job.";   
    }

    resource function post resumeAllJobs() returns error|string {
        check task:resumeAllJobs();
        return "Resumed all the jobs.";
    }

    resource function post pauseAllJobs() returns error|string {
        check task:pauseAllJobs();
        return "Paused all the jobs.";
    }

    resource function get getRunningJobs() returns int[] {
        task:JobId[] ids = task:getRunningJobs();
        int[] jobIds = [];
        foreach var id in ids {
            jobIds.push(id.id);
        } 
        return jobIds;
    }

    resource function post shutdown() {
        system_exit(0);
        return;
    }
}

function system_exit(int arg0) = @java:Method {
    name: "exit",
    'class: "java.lang.System",
    paramTypes: ["int"]
} external;