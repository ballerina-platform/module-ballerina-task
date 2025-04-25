// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/sql;
import ballerina/time;
import ballerinax/mysql;
import ballerinax/mysql.driver as _;
import ballerina/uuid;

# A read-only record consisting of a unique identifier for a created job.
public type JobId readonly & record {|
   int id;
|};

# The Ballerina Job object provides the abstraction for a job instance, which schedules to execute periodically.
public type Job object {

  # Executes by the Scheduler when the scheduled trigger fires.
  public function execute();
};

# Policies related to a trigger.
#
# + errorPolicy - The policy to follow when there is an error in Job execution
# + waitingPolicy - The policy to follow when the next task is triggering while the previous job is still
#                   being processing
public type TaskPolicy record {|
   ErrorPolicy errorPolicy = LOG_AND_TERMINATE;
   WaitingPolicy waitingPolicy = WAIT;
|};

# Possible options for the `ErrorPolicy`.
public enum ErrorPolicy {
  LOG_AND_TERMINATE,
  LOG_AND_CONTINUE,
  TERMINATE,
  CONTINUE
}

# Possible options for the `WaitingPolicy`.
public enum WaitingPolicy {
  WAIT,
  IGNORE,
  LOG_AND_IGNORE
}

# Represents the configuration required to connect to a database related to task coordination.
#
# + host - The hostname of the database server
# + user - The username for the database connection
# + password - The password for the database connection
# + port - The port number of the database server
# + database - The name of the database to connect to
# + options - Additional options for the MySQL connection
# + connectionPool - The connection pool configuration
public type DatabaseConfig record {
  string host = "localhost";
  string? user = ();
  string? password = ();
  int port = 3306;
  string? database = ();
  mysql:Options? options = ();
  sql:ConnectionPool? connectionPool = ();
};

# Represents the configuration required for task coordination.
#
# + databaseConfig - The database configuration for task coordination
# + livenessCheckInterval - The interval (in seconds) to check the liveness of the job. Default is 30 seconds.
# + nodeId - Optional identifier for the current node (auto-generated if not provided)
# + heartbeatFrequency - The interval (in seconds) for the node to update its heartbeat. Default is one second.
public type WarmBackupConfig record {
    DatabaseConfig databaseConfig = {};
    int livenessCheckInterval = 30;
    string nodeId = uuid:createRandomUuid();
    int heartbeatFrequency = 1;
};

# Gets time in milliseconds of the given `time:Civil`.
#
# + time - The Ballerina `time:Civil`
# + return - Time in milliseconds or else `task:Error` if the process failed due to any reason
public isolated function getTimeInMillies(time:Civil time) returns int|Error {
    if time[UTC_OFF_SET] == () {
        time:ZoneOffset zoneOffset = {hours: 0, minutes: 0};
        time.utcOffset = zoneOffset;
    }
    time:Utc|time:Error utc = time:utcFromCivil(time);
    if utc is time:Utc {
        if utc > time:utcNow() {
            return <int> decimal:round((<decimal>utc[0] + utc[1]) * 1000);
        }
        return error Error(
                    string `Invalid time: ${time.toString()}. Scheduled time should be greater than the current time`);
    } else {
        return error Error(string `Couldn't convert given time to milli seconds: ${utc.message()}.`);
    }
}

const string UTC_OFF_SET = "utcOffset";
