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

# Represents the task service.
public type Service distinct service object {
  isolated function onTrigger() returns error?;
};

# Worker count for the global scheduler
public configurable int globalSchedulerWorkerCount = 5;

# Waiting time for the global scheduler
public configurable time:Seconds globalSchedulerWaitingTime = 5;

# Listener configuration.
# 
# + schedule - The schedule configuration for the listener
public type ListenerConfiguration record {
  OneTimeConfiguration|RecurringConfiguration schedule;
};

# Recurring schedule configuration.
# 
# + interval - The duration of the trigger (in seconds), which is used to run the job frequently
# + maxCount - The maximum number of trigger counts
# + startTime - The trigger start time in Ballerina `time:Civil`. If it is not provided, a trigger will
#               start immediately
# + endTime - The trigger end time in Ballerina `time:Civil`
# + taskPolicy - The policy, which is used to handle the error and will be waiting during the trigger time
public type RecurringConfiguration record {|
  decimal interval;
  int maxCount = -1;
  time:Civil startTime?;
  time:Civil endTime?;
  TaskPolicy taskPolicy = {};
|};

# One-time schedule configuration.
# 
# + triggerTime - The specific time in Ballerina `time:Civil` to trigger only one time
public type OneTimeConfiguration record {|
  time:Civil triggerTime;
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

class TimeConverter {

  # Gets time in milliseconds of the given `time:Civil`.
  #
  # + time - The Ballerina `time:Civil`
  # + return - Time in milliseconds or else `task:Error` if the process failed due to any reason
  public isolated function getTimeInMillies(time:Civil time) returns int|Error {
      if time["utcOffset"] == () {
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
}
