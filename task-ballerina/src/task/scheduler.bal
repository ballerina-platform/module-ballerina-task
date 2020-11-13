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

import ballerina/java;

# Represents a ballerina task Scheduler, which can be used to schedule either a one-time or periodically
# job parallelly by using the given configurations.
public class Scheduler {

    private SchedulerConfiguration configuration;

    # Initializes a `task:Scheduler` object. This may panic if the initialization causes any error due
    # to a configuration error.
    # ```ballerina
    # Scheduler scheduler = new();
    # ```
    #
    # + config - The `task:ScedulerConfiguration`, which is used to initialize the `task:Scheduler`
    public isolated function init(SchedulerConfiguration configuration = {}) {
        validateSchedulerConfig(configuration);
        self.configuration = configuration;
        var result = initScheduler(self);
        if (result is SchedulerError) {
            panic result;
        }
    }

    # Schedules the given `job` according to the trigger config in the scheduler.
    # ```ballerina
    # Scheduler scheduler = new();
    # int count = 0;
    # var jobId = scheduler.scheduleJob(function() { count = count + 1; }, {intervalInMillis: 1000});
    # if (jobId is int) {
    #   io:println(id);
    # }
    # ```
    #
    # + job - Ballerina `service` or `function`, which is to be executed by the scheduler
    # + triggerConfig - The `task:SimpleTriggerConfiguration` or `task:CronTriggerConfiguration`, which is used to
    #                   configure the trigger of job.
    # + attachments - If you use the job as a `service`, set of optional parameters, which need to be
    #                 passed inside the resources
    # + return - A job id or else `task:SchedulerError` if the process failed due to any reason
    public isolated function scheduleJob(service|function () job, SimpleTriggerConfiguration|CronTriggerConfiguration
    triggerConfig, any... attachments) returns int|SchedulerError {
        validateConfiguration(triggerConfig);
        string message = "Failed to schedule the job in the scheduler";
        string|SchedulerError result = "";
        if (job is function ()) {
            Job functionPointer = new Job(job);
            result = scheduleJob(self, functionPointer, triggerConfig, ...attachments);
        } else {
             result = scheduleJob(self, job, triggerConfig, ...attachments);
        }
        if (result is SchedulerError) {
            return SchedulerError(message, result);
        } else {
            int|error id = 'int:fromString(result);
            if (id is int) {
                return id;
            } else {
                return SchedulerError(id.message());
            }
        }
    }

    # Starts the `task:Scheduler`. This may panic if the stopping causes any error.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.start();
    # ```
    #
    # + return - () or else a `task:SchedulerError` upon failure to start the listener
    public isolated function 'start() returns SchedulerError? {
        var result = startScheduler(self);
        if (result is SchedulerError) {
            string message = "Scheduler failed to start";
            return SchedulerError(message, result);
        }
    }

    # Removes the `job`, which is associated with the given job id
    # ```ballerina
    # Scheduler scheduler = new();
    # var jobId = scheduler.scheduleJob(function() { count = count + 1; }, {intervalInMillis: 1000});
    # checkpanic scheduler.start();
    # if (jobId is int) {
    #   checkpanic scheduler.rescheduleJob(jobId, {intervalInMillis: 2000});
    # }
    # ```
    #
    # + jobId - The id of the job, which needs to be reschedule
    # + triggerConfig - The `task:SimpleTriggerConfiguration` or `task:CronTriggerConfiguration` record to reschedule
    #                   the trigger for the job.
    # + return - A `task:SchedulerError` if the process failed due to any reason or else ()
    public isolated function rescheduleJob(int jobId, SimpleTriggerConfiguration|CronTriggerConfiguration triggerConfig)
    returns SchedulerError? {
        validateConfiguration(triggerConfig);
        return rescheduleJob(self, jobId, triggerConfig);
     }

    # Unschedule the the `job`, which is associated with the given job id.
    # ```ballerina
    # Scheduler scheduler = new();
    # var jobId = scheduler.scheduleJob(function() { count = count + 1; }, {intervalInMillis: 1000});
    # checkpanic scheduler.start();
    # if (jobId is int) {
    #   checkpanic scheduler.unScheduleJob(jobId);
    # }
    # ```
    #
    # + jobId - The id of the job, which needs to be unschedule
    # + return - A `task:SchedulerError` if the process failed due to any reason or else ()
    public isolated function unScheduleJob(int jobId) returns SchedulerError? {
        return unScheduleJob(self, jobId);
    }

    # Pauses the scheduler.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.start();
    # checkpanic scheduler.pause();
    # ```
    #
    # + return - A `task:SchedulerError` if an error is occurred while pausing or else ()
    public isolated function pause() returns SchedulerError? {
        return pause(self);
    }

    # Resumes a scheduler.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.start();
    # checkpanic scheduler.pause();
    # checkpanic scheduler.resume();
    # ```
    #
    # + return - A `task:SchedulerError` when an error occurred while resuming or else ()
    public isolated function resume() returns SchedulerError? {
        return resume(self);
    }

    # Pauses the particular job.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.start();
    # var jobId = scheduler.scheduleJob(function() { count = count + 1; }, {intervalInMillis: 1000});
    # if (jobId is int) {
    #   checkpanic scheduler.pauseJob(jobId);
    # }
    # ```
    #
    # + jobId - The id of the job, which needs to be pause
    # + return - A `task:SchedulerError` if an error is occurred while pausing a job or else ()
    public isolated function pauseJob(int jobId) returns SchedulerError? {
        return pauseJob(self, jobId);
    }

    # Resumes the particular job.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.start();
    # var jobId = scheduler.scheduleJob(function() { count = count + 1; }, {intervalInMillis: 1000});
    # if (jobId is int) {
    #   checkpanic scheduler.pauseJob(jobId);
    #   checkpanic scheduler.resumeJob(jobId);
    # }
    # ```
    #
    # + jobId - The id of the job, which needs to be resume
    # + return - A `task:SchedulerError` when an error occurred while resuming a job or else ()
    public isolated function resumeJob(int jobId) returns SchedulerError? {
        return resumeJob(self, jobId);
    }

    # Checks whether the scheduler is running or not.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.isRunning();
    # ```
    #
    # + return - `true` if the `Scheduler` is already started or else `false` if the `Scheduler` is not started yet or
    #             stopped calling the `Scheduler.stop()` function
    public isolated function isRunning() returns boolean {
        return isRunning(self);
    }

    # Gets all the running jobs.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.getAllRunningJobs(jobId);
    # ```
    #
    # + return - Returns all the running jobs' ids as an array
    public isolated function getAllRunningJobs() returns string[] {
        return getAllRunningJobs(self);
    }

    # Stops `task:Scheduler`. It will wait until all currently executing jobs have completed. This may panic if the
    # stopping causes any error.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.start();
    # checkpanic scheduler.gracefulStop();
    # ```
    #
    # + return - () or else a `task:SchedulerError` upon failure to stop the listener
    public isolated function gracefulStop() returns SchedulerError? {
        return gracefulStop(self);
    }

    # Stops the `task:Scheduler` immediately. This may panic if the stopping causes any error.
    # ```ballerina
    # Scheduler scheduler = new();
    # checkpanic scheduler.start();
    # checkpanic scheduler.stop();
    # ```
    #
    # + return - () or else a `task:SchedulerError` upon failure to stop the listener
    public isolated function stop() returns SchedulerError? {
        return stop(self);
    }
}

isolated function validateSchedulerConfig(SchedulerConfiguration config) {
    if (config.threadCount < 1) {
        panic ListenerError("Thread count must be greater than 0.");
    }
    if (config.threadPriority < 1 || config.threadPriority > 10) {
        panic ListenerError("Thread priority must be an integer value between 1 and 10.");
    }
    if (config.thresholdInMillis < 1) {
        panic ListenerError("Misfire threadshold value should be a positive integer.");
    }
}

isolated function pause(Scheduler task) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function resume(Scheduler task) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function pauseJob(Scheduler task, int jobId) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function resumeJob(Scheduler task, int jobId) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function gracefulStop(Scheduler task) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function stop(Scheduler task) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function startScheduler(Scheduler task) returns SchedulerError? = @java:Method {
    name: "start",
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function initScheduler(Scheduler task) returns SchedulerError? = @java:Method {
    name: "init",
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function getAllRunningJobs(Scheduler task) returns string[] = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function scheduleJob(Scheduler task, service|Job job, SimpleTriggerConfiguration|CronTriggerConfiguration
triggerConfig, any... attachments) returns string|SchedulerError = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function rescheduleJob(Scheduler task, int jobId, SimpleTriggerConfiguration|CronTriggerConfiguration
triggerConfig) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function unScheduleJob(Scheduler task, int jobId) returns SchedulerError? = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function isRunning(Scheduler task) returns boolean = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;
