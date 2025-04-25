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

import ballerina/jballerina.java;
import ballerina/time;

# Configure the scheduler worker pool.
# ```ballerina
# check task:configureWorkerPool(4, 7);
# ```
#
# + workerCount - Specifies the number of workers that are available for the concurrent execution of jobs.
#                 It should be a positive integer. The recommendation is to set a value less than 10. Default sets to 5.
# + waitingTime - The number of seconds as a decimal the scheduler will tolerate a trigger to pass its next-fire-time
#                 before being considered as `ignored the trigger`
# + return - A `task:Error` if the process failed due to any reason or else ()
public isolated function configureWorkerPool(int workerCount = 5, time:Seconds waitingTime = 5)
                                returns Error? {
    return configureThread(workerCount, <int>(waitingTime * <decimal>1000.0));
}

# Schedule the given `task:Job` for the given time. Once scheduled, it will return a job ID, which can be used to manage
# the job.
# ```ballerina
# time:Utc newTime = time:utcAddSeconds(time:utcNow(), 3);
# time:Civil time = time:utcToCivil(newTime);
# task:JobId jobId = check task:scheduleOneTimeJob(new Job(), time);
# ```
#
# + triggerTime - The specific time in Ballerina `time:Civil` to trigger only one time
# + job - Ballerina job, which is to be executed during the trigger
# + return - A `task:JobId` or else a `task:Error` if the process failed due to any reason
public isolated function scheduleOneTimeJob(Job job, time:Civil triggerTime) returns JobId|Error {
    int result = check scheduleJob(job, check getTimeInMillies(triggerTime));
    JobId jobId = {id: result};
    return jobId;
}

# Schedule the recurring `task:Job` according to the given duration. Once scheduled, it will return the job ID, which
# can be used to manage the job.
# ```ballerina
# task:JobId jobId = check task:scheduleJobRecurByFrequency(new Job(), 3);
# ```
#
# + job - Ballerina job, which is to be executed by the scheduler
# + interval - The duration of the trigger (in seconds), which is used to run the job frequently
# + maxCount - The maximum number of trigger counts
# + startTime - The trigger start time in Ballerina `time:Civil`. If it is not provided, a trigger will
#               start immediately
# + endTime - The trigger end time in Ballerina `time:Civil`
# + taskPolicy -  The policy, which is used to handle the error and will be waiting during the trigger time
# + warmBackupConfig - The configuration related to task coordination
#   When using multiple nodes, the duration must be different from other nodes.
# + return - A `task:JobId` or else a `task:Error` if the process failed due to any reason
public isolated function scheduleJobRecurByFrequency(Job job,  decimal interval,  int maxCount = -1,
                                    time:Civil? startTime = (), time:Civil? endTime = (), TaskPolicy taskPolicy = {},
                                    WarmBackupConfig? warmBackupConfig = ()) returns JobId|Error {
    if maxCount != -1 && maxCount < 1 {
        return error Error("The maxCount should be a positive integer.");
    }
    int? sTime = ();
    int? eTime = ();
    if startTime is time:Civil {
        sTime = check getTimeInMillies(startTime);
    }
    if endTime is time:Civil {
        eTime = check getTimeInMillies(endTime);
    }
    int result;
    if warmBackupConfig !is () {
        string instanceId = warmBackupConfig.nodeId;
        boolean tokenAcquired = false;
        map<any> response;
        do {
            response = check acquireToken(warmBackupConfig.databaseConfig, instanceId, 
                                          tokenAcquired, warmBackupConfig.livenessCheckInterval, 
                                          warmBackupConfig.heartbeatFrequency);
        } on fail error err {
            return error Error(err.message());
        }
        result = check scheduleIntervalJobWithToken(job, interval, maxCount, sTime, eTime, taskPolicy, response);
    } else {
        result = check scheduleIntervalJob(job, interval, maxCount, sTime, eTime, taskPolicy);
    }
    JobId jobId = {id: result};
    return jobId;
}

# Unschedule the `task:Job`, which is associated with the given job ID. If no job is running in the scheduler,
# the scheduler will be shut down automatically.
# ```ballerina
# check task:unscheduleJob(jobId);
# ```
#
# + jobId - The ID of the job as a `task:JobId`, which needs to be unscheduled
# + return - A `task:Error` if the process failed due to any reason or else ()
public isolated function unscheduleJob(JobId jobId) returns Error? {
    return externUnscheduleJob(jobId.id);
}

# Pauses all the jobs.
# ```ballerina
# check task:pauseAllJobs();
# ```
#
# + return - A `task:Error` if an error occurred while pausing or else ()
public isolated function pauseAllJobs() returns Error? {
    return externPauseAllJobs();
}

# Resumes all the jobs.
# ```ballerina
# check task:resumeAllJobs();
# ```
#
# + return - A `task:Error` when an error occurred while resuming or else ()
public isolated function resumeAllJobs() returns Error? {
    return externResumeAllJobs();
}

# Pauses the particular job.
# ```ballerina
# check task:pauseJob(jobId);
# ```
#
# + jobId - The ID of the job as a `task:JobId`, which needs to be paused
# + return - A `task:Error` if an error occurred while pausing a job or else ()
public isolated function pauseJob(JobId jobId) returns Error? {
    return externPauseJob(jobId.id);
}

# Resumes the particular job.
# ```ballerina
# check task:resumeJob(jobId);
# ```
#
# + jobId - The ID of the job as a `task:JobId`, which needs to be resumed
# + return - A `task:Error` when an error occurred while resuming a job or else ()
public isolated function resumeJob(JobId jobId) returns Error? {
    return externResumeJob(jobId.id);
}

# Gets all the running jobs.
# ```ballerina
# task:JobId[] result = task:getRunningJobs();
# ```
#
# + return - Returns the IDs of all the running jobs as an array
public isolated function getRunningJobs() returns JobId[] {
    JobId[] jobIds = [];
    int[] ids = externGetRunningJobs();
    int i = 0;
    foreach int id in ids {
        jobIds[i] = {id: id};
        i += 1;
    }
    return jobIds;
}

isolated function scheduleJob(Job job, int triggerTime) returns int|Error = @java:Method {
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function configureThread(int workerCount, int waitingTime) returns Error? =
@java:Method {
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function scheduleIntervalJob(Job job, decimal interval, int maxcount, int? startTime, int? endTime,
TaskPolicy taskPolicy) returns int|Error = @java:Method {
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function scheduleIntervalJobWithToken(Job job, decimal interval, int maxcount, int? startTime, int? endTime,
TaskPolicy taskPolicy, map<any> response) returns int|Error = @java:Method {
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function externUnscheduleJob(int id) returns Error? = @java:Method {
    name: "unscheduleJob",
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function externPauseAllJobs() returns Error? = @java:Method {
    name: "pauseAllJobs",
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function externResumeAllJobs() returns Error? = @java:Method {
    name: "resumeAllJobs",
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function externPauseJob(int id) returns Error? = @java:Method {
    name: "pauseJob",
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function externResumeJob(int id) returns Error? = @java:Method {
    name: "resumeJob",
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function externGetRunningJobs() returns int[] = @java:Method {
    name: "getRunningJobs",
    'class: "io.ballerina.stdlib.task.actions.TaskActions"
} external;

isolated function acquireToken(DatabaseConfig dbConfig, string instanceId, boolean tokenAcquired, int livenessInterval,
                               int heartbeatFrequency) returns map<anydata>|error = @java:Method {
    'class: "io.ballerina.stdlib.task.TokenAcquisition"
} external;
