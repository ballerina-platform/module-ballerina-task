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

# Configurations related to a timer, which are used to define the behavior of a timer when initializing the
# `task:Listener`.
#
# + intervalInMillis - Timer interval (in milliseconds), which triggers the `onTrigger` resource
# + initialDelayInMillis - Delay (in milliseconds) after which the timer will run
# + noOfRecurrences - Number of times to trigger the task after which the task stops running
# + thresholdInMillis - The number of milliseconds the scheduler will tolerate a trigger to pass its
#                       next-fire-time by being considered `misfired` before.
# + misfirePolicy - The policy, which is used to inform what it should do when a misfire occurs. The following are the
#            scenarios in which the policy can be used:
#               One-time task (the task will be run once):
#                   smartPolicy - This is the default policy, which will act as `firenow`
#                   fireNow - fireNow - Instructs the scheduler if the Trigger misfires. Then, the trigger wants
#                             to be fired now by the scheduler.
#                   ignorePolicy - If the Trigger misfires, this instructs the scheduler that the trigger will
#                                  never be evaluated for a misfire situation and that the scheduler will
#                                  simply try to fire it as soon as it can and then update the trigger as
#                                  if it had fired at the proper time.
#               Recurrinng task(the task will be run repeatedly):
#                   smartPolicy - This is the default policy. If the repeat count is indefinite, this will act
#                                 as the `rescheduleNextWithRemainigCount`. Else, it will act as the
#                                 `rescheduleNowWithExistingRepeatCount`.
#                   fireNextWithExistingCount - Instructs the scheduler if the trigger misfires. Then,
#                                               the trigger wants to be re-scheduled to the next
#                                               scheduled time after 'now' and with the repeat count
#                                               left unchanged.
#                   fireNextWithRemainingCount - Instructs the scheduler if the trigger misfires. Then,
#                                                the trigger wants to be re-scheduled to the next scheduled time
#                                                after 'now' and with the repeat count set to what it would be
#                                                if it had not missed any firings.
#                   fireNowWithExistingCount - Instructs the scheduler if the trigger misfires. Then,
#                                              the trigger wants to be re-scheduled to 'now' with the
#                                              repeat count left as it is. If 'now' is after the end-time
#                                              the Trigger will not fire again as this does obey
#                                              the Trigger end-time.
#                   fireNowWithRemainingCount - Instructs the scheduler if the trigger misfires. Then,
#                                               the trigger wants to be re-scheduled to
#                                               'now' with the repeat count set to what it
#                                               would be if it had not missed any firings.
#                   ignorePolicy - If the trigger misfires, this instructs the scheduler that the trigger will
#                                  never be evaluated for a misfire situation and that the scheduler will
#                                  simply try to fire it as soon as it can and then update the Trigger
#                                  as if it had fired at the proper time.
public type TimerConfiguration record {|
    int intervalInMillis;
    int initialDelayInMillis?;
    int noOfRecurrences?;
    int thresholdInMillis = 5000;
    TimerMisfirePolicy misfirePolicy = "smartPolicy";
|};

# Configurations related to a ThreadPool.
#
# + threadCount - Specifies the number of threads that are available for the concurrent execution of jobs.
#                 It can be set to a positive integer between 1 and 10. Values greater than 10 are allowed but
#                 might be impractical.
# + threadPriority - Specifies the priority that the worker threads run at. The value can be any integer
#                    between 1 and 10. The default is 5.
public type ThreadConfiguration record {|
    int threadCount = 10;
    int threadPriority = 5;
|};

# Configurations related to an appointment, which are used to define the behavior of an appointment when initializing
# the `task:Listener`.
#
# + cronExpression - A CRON expression (eg: `* * * * ? *`) as a string for scheduling an appointment that is made up
#                    of six or seven sub-expressions that describe individual details of the schedule. This
#                    sub-expression is separated with white-space, and represent:
#                       Seconds
#                       Minutes
#                       Hours
#                       Day-of-Month
#                       Month
#                       Day-of-Week
#                       Year (optional field)
# + noOfRecurrences - Number of times to trigger the task after which the task stops running
# + thresholdInMillis - The number of milliseconds the scheduler will tolerate a trigger to pass its
#                       next-fire-time by being considered `misfired` before.
# + misfirePolicy - The policy, which is used to inform what it should do when a misfire occurs. The following are the
#                   scenarios in which the policy can be used:
#                       smartPolicy - This is the default policy, which will act as the `FireAndProceed`
#                       ignorePolicy - If the Trigger misfires, this instructs the scheduler that the trigger will
#                                      never be evaluated for a misfire situation and that the scheduler will
#                                      simply try to fire it as soon as it can and then update the trigger as
#                                      if it had fired at the proper time.
#                       doNothing - Instructs the scheduler if the trigger misfires. Then, the trigger wants to have
#                                   it's next-fire-time updated to the next time in the schedule after the current time.
#                       fireAndProceed - Instructs the scheduler if the trigger misfires. Then, the trigger wants
#                                        to be fired now by the scheduler.
public type AppointmentConfiguration record {|
    string cronExpression;
    int noOfRecurrences?;
    int thresholdInMillis = 5000;
    AppointmentMisfirePolicy misfirePolicy = "smartPolicy";
|};

# Possible types of parameters that can be passed into the `TimerTaskPolicy`.
public type TimerMisfirePolicy RecurringTaskPolicy|OneTimeTaskPolicy;

# Possible types of parameters that can be passed into the `AppointmentTaskPolicy`.
public type AppointmentMisfirePolicy SMART_POLICY|DO_NOTHING|FIRE_AND_PROCEED|IGNORE_POLICY;

# Possible types of parameters that can be passed into the `RecurringTaskPolicy`.
public type RecurringTaskPolicy SMART_POLICY|IGNORE_POLICY|FIRE_NEXT_WITH_EXISTING_COUNT|
FIRE_NEXT_WITH_REMAINING_COUNT|FIRE_NOW_WITH_EXISTING_COUNT|FIRE_NOW_WITH_REMAINING_COUNT;

# Possible types of parameters that can be passed into the `OneTimeTaskPolicy`.
public type OneTimeTaskPolicy SMART_POLICY|IGNORE_POLICY|FIRE_NOW;

# The value of this constant is `smartPolicy`.
public const string SMART_POLICY = "smartPolicy";
# The value of this constant is `doNothing`.
public const string DO_NOTHING = "doNothing";
# The value of this constant is `fireAndProceed`.
public const string FIRE_AND_PROCEED = "fireAndProceed";
# The value of this constant is `fireNextWithExistingCount`.
public const string FIRE_NEXT_WITH_EXISTING_COUNT = "fireNextWithExistingCount";
# The value of this constant is `fireNextWithRemainingCount`.
public const string FIRE_NEXT_WITH_REMAINING_COUNT = "fireNextWithRemainingCount";
# The value of this constant is `fireNowWithExistingCount`.
public const string FIRE_NOW_WITH_EXISTING_COUNT = "fireNowWithExistingCount";
# The value of this constant is `fireNowWithRemainingCount`.
public const string FIRE_NOW_WITH_REMAINING_COUNT = "fireNowWithRemainingCount";
# The value of this constant is `fireNow`.
public const string FIRE_NOW = "fireNow";
# The value of this constant is `ignorePolicy`.
public const string IGNORE_POLICY = "ignorePolicy";

const string NO_OF_RECURRENCE = "noOfRecurrences";
const string INITIAL_DELAY = "initialDelayInMillis";
