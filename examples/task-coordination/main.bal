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

import ballerina/io;
import ballerina/task;
import ballerina/time;
import ballerinax/postgresql;
import ballerinax/postgresql.driver as _;

configurable task:PostgresqlConfig databaseConfig = ?;
configurable int livenessCheckInterval = ?;
configurable int heartbeatFrequency = ?;
configurable string taskId = ?;
configurable string groupId = ?;

time:Utc currentUtc = time:utcNow();

final postgresql:Client dbClient = check new (username = databaseConfig.user, password = databaseConfig.password, database = "testdb");

listener task:Listener taskListener = new (
    trigger = {
        interval: 4,
        maxCount: 20,
        startTime: time:utcToCivil(time:utcAddSeconds(currentUtc, 5)),
        endTime: time:utcToCivil(time:utcAddSeconds(currentUtc, 60)),
        taskPolicy: {}
    },
    warmBackupConfig = {
        databaseConfig,
        livenessCheckInterval,
        taskId,
        groupId,
        heartbeatFrequency
    }
);

service "job-1" on taskListener {
    private int i = 1;

    isolated function execute() returns error? {
        lock {
            self.i += 1;
            io:println("Counter: ", self.i);
        }
    }
}
