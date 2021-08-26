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

import ballerina/log;
import ballerina/email;
import ballerina/lang.runtime;
import ballerina/task;
import ballerina/time;

configurable string host = ?;
configurable string username = ?;
configurable string password = ?;
configurable string toAddress = ?;
configurable string subject = ?;
configurable string body = ?;
configurable int port = ?;

// Create the email client
email:SmtpClient smtpClient = check new (host, username , password, port = port);

// Creates a job to be executed by the scheduler.
class Job {

    *task:Job;

    // Executes this function when the scheduled trigger fires.
    public function execute() {
        // Creates a message
        email:Message email = {
            to: [toAddress],
            subject: subject,
            body: body
        };
        // Sends email
        email:Error? sendMessage = smtpClient->sendMessage(email);
        if (sendMessage is email:Error) {
            log:printError("Error: ", sendMessage);
        } else {
            log:printInfo("The email has been sent now.");
        }
    }
}

public function main() returns error? {

    // Sets trigger time
    time:Civil time = {
        year: 2021,
        month: 8,
        day: 26,
        hour: 12,
        minute: 38,
        second: 50.52,
        timeAbbrev: "Asia/Colombo",
        utcOffset: {hours: 5, minutes: 30}
    };

    // Schedules the one time job
    task:JobId id = check task:scheduleOneTimeJob(new Job(), time);

    // Calculate the waiting time.
    time:Seconds seconds = time:utcDiffSeconds(check time:utcFromCivil(time), time:utcNow());

    // Waits until executing the job.
    runtime:sleep((seconds + 1));
}   
