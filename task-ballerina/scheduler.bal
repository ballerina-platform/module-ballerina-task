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
import ballerina/log;

# Represents a ballerina task Scheduler, which can be used to schedule either a one-time or periodically
# job parallelly by using the given configurations.
public class Scheduler {

    private int threadCount = 0;
    private int threadPriority = 0;
    private int thresholdInMillis = 0;

    # Initializes a `task:Scheduler` object. This may panic if the initialization causes any error.
    #
    # + config - The `task:ScedulerConfiguration`, which is used to initialize the `task:Scheduler`
    isolated function init() {}

    # Create a `task:Scheduler` object. This may panic if the creation causes any error.
    #
    # + threadCount - Specifies the number of threads that are available for the concurrent execution of jobs.
    #                 It can be set to a positive integer. Values greater than 10 are allowed but might be impractical.
    # + threadPriority - Specifies the priority that the worker threads run at. The value can be any integer between
    #                    1 and 10. The default is 5.
    # + thresholdInMillis - The number of milliseconds the scheduler will tolerate a trigger to pass its next-fire-time
    #                       by being considered `misfired` before.
    isolated function create(int threadCount, int threadPriority, int thresholdInMillis) {
        validateSchedulerConfig(threadCount, threadPriority, thresholdInMillis);
        self.threadCount = threadCount;
        self.threadPriority = threadPriority;
        self.thresholdInMillis = thresholdInMillis;
        var result = initScheduler(self);
        if (result is SchedulerError) {
            panic result;
        }
    }


    # Schedules the given `job` according to the trigger config in the scheduler. The scheduler will be automatically
    # started when the first job schedules to the scheduler.
    # ```ballerina
    # int|task:Error jobId = scheduler.scheduleJob(function() { // Adding executing logic}, {intervalInMillis: 1000});
    # ```
    #
    # + job - Ballerina `service` or `function`, which is to be executed by the scheduler.
    # + triggerConfig - The `task:SimpleTriggerConfiguration` or `task:CronTriggerConfiguration`, which is used to
    #                   configure the trigger of job.
    # + return - A job id or else `task:SchedulerError` if the process failed due to any reason
    public isolated function scheduleJob(function () job, SimpleTriggerConfiguration|CronTriggerConfiguration
    triggerConfig) returns int|SchedulerError {
        validateConfiguration(triggerConfig);
        string message = "Failed to schedule the job in the scheduler";
        string|SchedulerError result = "";
        Job functionPointer = new Job(job);
        result = scheduleJob(self, functionPointer, triggerConfig);
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

    # Reschedules the `job`, which is associated with the given job id.
    # ```ballerina
    # task:Error? jobId = scheduler.scheduleJob(function() { count = count + 1; }, {intervalInMillis: 1000});
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
    # task:Error? jobId = scheduler.scheduleJob(function() { count = count + 1; }, {intervalInMillis: 1000});
    # ```
    #
    # + jobId - The id of the job, which needs to be unschedule
    # + return - A `task:SchedulerError` if the process failed due to any reason or else ()
    public isolated function unScheduleJob(int jobId) returns SchedulerError? {
        return unScheduleJob(self, jobId);
    }

    # Pauses the scheduler.
    # ```ballerina
    # task:Error? result = scheduler.pauseAllJobs();
    # ```
    #
    # + return - A `task:SchedulerError` if an error is occurred while pausing or else ()
    public isolated function pauseAllJobs() returns SchedulerError? {
        return pause(self);
    }

    # Resumes a scheduler. If any of the Jobs missed one or more fire-times, then misfire instruction will be
    # applied to those jobs.
    # ```ballerina
    # task:Error? result = scheduler.resumeAllJobs();
    # ```
    #
    # + return - A `task:SchedulerError` when an error occurred while resuming or else ()
    public isolated function resumeAllJobs() returns SchedulerError? {
        return resume(self);
    }

    # Pauses the particular job.
    # ```ballerina
    # task:Error? result = scheduler.pauseJob();
    # ```
    #
    # + jobId - The id of the job, which needs to be pause
    # + return - A `task:SchedulerError` if an error is occurred while pausing a job or else ()
    public isolated function pauseJob(int jobId) returns SchedulerError? {
        return pauseJob(self, jobId);
    }

    # Resumes the particular job. If any of the Jobs missed one or more fire-times, then misfire instruction will be
    # applied to those jobs.
    # ```ballerina
    # task:Error? result = scheduler.resumeJob();
    # ```
    #
    # + jobId - The id of the job, which needs to be resume
    # + return - A `task:SchedulerError` when an error occurred while resuming a job or else ()
    public isolated function resumeJob(int jobId) returns SchedulerError? {
        return resumeJob(self, jobId);
    }

    # Checks whether the scheduler is running or not.
    # ```ballerina
    # task:Error? result = scheduler.isRunning();
    # ```
    #
    # + return - `true` if the `Scheduler` is already started or else `false` if the `Scheduler` is not started yet or
    #             stopped calling the `Scheduler.stop()` function
    public isolated function isRunning() returns boolean {
        return isRunning(self);
    }

    # Gets all the running jobs.
    # ```ballerina
    # task:Error? result = scheduler.getRunningJobs();
    # ```
    #
    # + return - Returns all the running jobs' ids as an array
    public isolated function getRunningJobs() returns string[] {
        return getAllRunningJobs(self);
    }

    # Stops the `task:Scheduler`. It will wait until all currently executing jobs have completed. This may panic if the
    # stopping causes any error.
    # ```ballerina
    # task:Error? result = scheduler.gracefulStop();
    # ```
    #
    # + return - () or else a `task:SchedulerError` upon failure to stop the listener
    public function gracefulStop() returns SchedulerError? {
        isSchedulerCreated = false;
        return gracefulStop(self);
    }

    # Stops the `task:Scheduler` immediately. This may panic if the stopping causes any error.
    # ```ballerina
    # task:Error? result = scheduler.stop();
    # ```
    #
    # + return - () or else a `task:SchedulerError` upon failure to stop the listener
    public function stop() returns SchedulerError? {
        isSchedulerCreated = false;
        return stop(self);
    }
}

isolated function validateSchedulerConfig(int threadCount, int threadPriority, int thresholdInMillis) {
    if (threadCount < 1) {
        panic ListenerError("Thread count must be greater than 0.");
    }
    if (threadPriority < 1 || threadPriority > 10) {
        panic ListenerError("Thread priority must be an integer value between 1 and 10.");
    }
    if (thresholdInMillis < 1) {
        panic ListenerError("Misfire threadshold value should be a positive integer.");
    }
}

boolean isSchedulerCreated = false;
Scheduler schedulerInst = new();

# Initializes and gets a `task:Scheduler`.
# ```ballerina
# Scheduler scheduler = scheduler();
# ```
#
# + threadCount - Specifies the number of threads that are available for the concurrent execution of jobs.
#                 It can be set to a positive integer. Values greater than 10 are allowed but might be impractical.
# + threadPriority - Specifies the priority that the worker threads run at. The value can be any integer between
#                    1 and 10. The default is 5.
# + thresholdInMillis - The number of milliseconds the scheduler will tolerate a trigger to pass its next-fire-time
#                       by being considered `misfired` before.
# + return -  A `task:Scheduler`
public function scheduler(int threadCount = 5, int threadPriority = 5, int thresholdInMillis = 5000) returns Scheduler {
    if (!isSchedulerCreated) {
       schedulerInst.create(threadCount, threadPriority, thresholdInMillis);
       isSchedulerCreated = true;
    } else {
        log:printWarn("Unable to create new scheduler as the scheduler is already created.");
    }
    return schedulerInst;
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

isolated function initScheduler(Scheduler task) returns SchedulerError? = @java:Method {
    name: "init",
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function getAllRunningJobs(Scheduler task) returns string[] = @java:Method {
    'class: "org.ballerinalang.stdlib.task.actions.SchedulerActions"
} external;

isolated function scheduleJob(Scheduler task, Job job, SimpleTriggerConfiguration|CronTriggerConfiguration
triggerConfig) returns string|SchedulerError = @java:Method {
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
