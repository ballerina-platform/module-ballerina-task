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

import ballerina/time;
import ballerina/crypto;

configurable int maxOpenConnections = 15;
configurable decimal maxConnectionLifeTime = 1800.0;
configurable int minIdleConnections = 15;

# Represents the properties, which are used to configure a DB connection pool.
# Default values of the fields can be set through the configuration API.
#
# + maxOpenConnections - The maximum number of open connections that the pool is allowed to have.
#                        Includes both idle and in-use connections. The default value is 15. This can be changed through
#                        the configuration API with the `ballerina.sql.maxOpenConnections` key
# + maxConnectionLifeTime - The maximum lifetime (in seconds) of a connection in the pool. The default value is 1800
#                           seconds (30 minutes). A value of 0 indicates an unlimited maximum lifetime (infinite lifetime).
#                           The minimum allowed value is 30 seconds. This can be changed through the configuration API
#                           with the `ballerina.sql.maxConnectionLifeTime` key.
# + minIdleConnections - The minimum number of idle connections that the pool tries to maintain. The default value
#                        is the same as `maxOpenConnections` and it can be changed through the configuration
#                        API with the `ballerina.sql.minIdleConnections` key
public type ConnectionPool record {|
    int maxOpenConnections = maxOpenConnections;
    decimal maxConnectionLifeTime = maxConnectionLifeTime;
    int minIdleConnections = minIdleConnections;
|};

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
public type DatabaseConfig MysqlConfig|PostgresqlConfig;

# Represents the configuration required to connect to a database related to task coordination.
#
# + host - The hostname of the database server
# + user - The username for the database connection
# + password - The password for the database connection
# + port - The port number of the database server
# + database - The name of the database to connect to
# + options - Additional options for the database connection
# + connectionPool - The connection pool configuration
public type MysqlConfig record {
  string host = "localhost";
  string? user = ();
  string? password = ();
  int port = 3306;
  string? database = ();
  Options? options = ();
  ConnectionPool? connectionPool = ();
};

# Represents the configuration required to connect to a database related to task coordination.
#
# + host - The hostname of the database server
# + user - The username for the database connection
# + password - The password for the database connection
# + port - The port number of the database server
# + database - The name of the database to connect to
# + options - Additional options for the database connection
# + connectionPool - The connection pool configuration
public type PostgresqlConfig record {
  string host = "localhost";
  string? user = ();
  string? password = ();
  int port = 5432;
  string? database = ();
  Options? options = ();
  ConnectionPool? connectionPool = ();
};

# Provides a set of additional configurations related to the database connection.
#
# + ssl - SSL configurations to be used
# + failoverConfig - Server failover configurations to be used
# + useXADatasource - Flag to enable or disable XADatasource
# + connectTimeout - Timeout (in seconds) to be used when establishing a connection to the database server
# + socketTimeout - Socket timeout (in seconds) to be used during the read/write operations with the database server
#                   (0 means no socket timeout)
# + serverTimezone - Configures the connection time zone, which is used by the `Connector/J` if the conversion between a Ballerina
#                    application and a target time zone is required when preserving instant temporal values
# + noAccessToProcedureBodies - With this option the user is allowed to invoke procedures with access to metadata restricted
public type Options record {|
    SecureSocket ssl?;
    FailoverConfig failoverConfig?;
    boolean useXADatasource = false;
    decimal connectTimeout = 30;
    decimal socketTimeout = 0;
    string serverTimezone?;
    boolean noAccessToProcedureBodies = false;
|};

public type SecureSocket record {|
    SSLMode mode = SSL_PREFERRED;
    crypto:KeyStore key?;
    crypto:TrustStore cert?;
    boolean allowPublicKeyRetrieval = false;
|};

# Establish an encrypted connection if the server supports encrypted connections. Falls back to an unencrypted
# connection if an encrypted connection cannot be established.
public const SSL_PREFERRED = "PREFERRED";

# Establish an encrypted connection if the server supports encrypted connections. The connection attempt fails if
# an encrypted connection cannot be established.
public const SSL_REQUIRED = "REQUIRED";

# Establish an encrypted connection if the server supports encrypted connections. The connection attempt fails if
# an encrypted connection cannot be established. Additionally, verifies the server Certificate Authority (CA)
# certificate against the configured CA certificates. The connection attempt fails if no valid matching CA
# certificates are found.
public const SSL_VERIFY_CA = "VERIFY_CA";

# Establish an encrypted connection if the server supports encrypted connections and verifies the server
# Certificate Authority (CA) certificate against the configured CA certificates. The connection attempt fails if an
# encrypted connection cannot be established or no valid matching CA certificates are found. Also, performs hostname
# identity verification by checking the hostname the client uses for connecting to the server against the identity
# in the certificate that the server sends to the client.
public const SSL_VERIFY_IDENTITY = "VERIFY_IDENTITY";

# Establish an unencrypted connection to the server. If the server supports encrypted connections, the connection
# attempt fails.
public const SSL_DISABLED = "DISABLED";

# `SSLMode` as a union of available SSL modes.
public type SSLMode SSL_PREFERRED|SSL_REQUIRED|SSL_VERIFY_CA|SSL_VERIFY_IDENTITY|SSL_DISABLED;

# Configuration to be used for server failover.
#
# + failoverServers - Array of `FailoverServer` for the secondary servers
# + timeBeforeRetry - Time the driver waits before attempting to fall back to the primary host
# + queriesBeforeRetry - Number of queries that are executed before the driver attempts to fall back to the primary host
# + failoverReadOnly - Open connection to secondary host with READ ONLY mode.
public type FailoverConfig record {|
    FailoverServer[] failoverServers;
    int timeBeforeRetry?;
    int queriesBeforeRetry?;
    boolean failoverReadOnly = true;
|};

# Configuration for failover servers
#
# + host - Hostname of the secondary server
# + port - Port of the secondary server
public type FailoverServer record {|
    string host;
    int port;
|};

# Represents the configuration required for task coordination.
#
# + databaseConfig - The database configuration for task coordination
# + livenessCheckInterval - The interval (in seconds) to check the liveness of the job. Default is 30 seconds.
# + taskId - Unique identifier for the current task
# + groupId - The identifier for the group of tasks. This is used to identify the group of tasks that are
#             coordinating the task. It is recommended to use a unique identifier for each group of tasks.
# + heartbeatFrequency - The interval (in seconds) for the node to update its heartbeat. Default is one second.
public type WarmBackupConfig record {
    DatabaseConfig databaseConfig = <MysqlConfig>{};
    int livenessCheckInterval = 30;
    string taskId;
    string groupId;
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
