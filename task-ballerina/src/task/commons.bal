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
public type TimerConfiguration record {|
    int intervalInMillis;
    int initialDelayInMillis?;
    int noOfRecurrences?;
|};

# Configurations related to an appointment, which are used to define the behavior of an appointment when initializing
# the `task:Listener`.
#
# + appointmentDetails - A CRON expression as a string or `task:AppointmentData` for scheduling an appointment
# + noOfRecurrences - Number of times to trigger the task after which the task stops running
public type AppointmentConfiguration record {|
    string|AppointmentData appointmentDetails;
    int noOfRecurrences?;
|};

# The CRON expression required for scheduling an appointment.
#
# + seconds - Second(s) in a given minute in which the appointment will run
# + minutes - Minute(s) in a given hour in which the appointment will run
# + hours - Hour(s) in a given day in which the appointment will run
# + daysOfMonth - Day(s) of the month in which the appointment will run
# + months - Month(s) in a given year in which the appointment will run
# + daysOfWeek - Day(s) of a week in which the appointment will run
# + year - Year(s) in which the appointment will run
public type AppointmentData record {|
    string seconds?;
    string minutes?;
    string hours?;
    string daysOfMonth?;
    string months?;
    string daysOfWeek?;
    string year?;
|};

# Represents configurations for the `task` objefct.
#
# + triggerConfig - The `task:TimerConfiguration` or `task:AppointmentConfiguration` record which is used to define
#                   the task trigger behavior
# + misfireConfig - The `task:MisfireConfig` record which is used to configure the misfire policy
public type TaskConfiguration record {|
    AppointmentConfiguration|TimerConfiguration triggerConfig;
    MisfireConfiguration misfireConfig = {};
|};

# Represents configurations for the misfire situations of the scheduler.
#
# + thresholdInMillis - The number of milliseconds the scheduler will tolerate a trigger to pass its
#                       next-fire-time by, before being considered `misfired`.
# + instruction - The instruction which is used to inform what it should do when a misfire occurs. Following are the
#                 instruction can be used:
#                 For one-off trigger instruction:
#                     smartPolicy - This is default which will act as `firenow`
#                     fireNow - Instructs the Scheduler If the Trigger misfires, the trigger wants to be fired
#                               now by Scheduler.
#                     ignoreMisfiresPoilcy - If the Trigger misfires, instructs the Scheduler that the trigger will
#                                            never be evaluated for a misfire situation and that the scheduler will
#                                            simply try to fire it as soon as it can, and then update the trigger as
#                                            if it had fired at the proper time.
#                 For recurring trigger:
#                     smartPolicy - This is default. If the repeat count is indefinite, will act as
#                                   `rescheduleNextWithRemainigCount`, else will act as
#                                   `rescheduleNowWithExistingRepeatCount`.
#                     rescheduleNextWithExistingCount - Instructs the Scheduler if the trigger misfires,
#                                                       the SimpleTrigger wants to be re-scheduled to the next
#                                                       scheduled time after 'now', and with the repeat count
#                                                       left unchanged.
#                     rescheduleNextWithRemainingCount - Instructs the Scheduler if the trigger misfires, the trigger
#                                                       wants to be re-scheduled to the next scheduled time after
#                                                       'now', and with the repeat count set to what it would be,
#                                                       if it had not missed any firings.
#                     rescheduleNowWithExistingRepeatCount - Instructs the Scheduler if the Trigger misfires,
#                                                            the trigger wants to be re-scheduled to 'now' with the
#                                                            repeat count left as-is. If 'now' is after the end-time
#                                                            the Trigger will not fire again as this does obey
#                                                            the Trigger end-time.
#                     handlingInstructionNowWithRemainingRepeatCount - Instructs the Scheduler if the Trigger misfires,
#                                                                      the SimpleTrigger wants to be re-scheduled to
#                                                                      'now' with the repeat count set to what it
#                                                                       would be, if it had not missed any firings.
#                     ignoreMisfiresPoilcy - If the Trigger misfires, instructs the Scheduler that the trigger will
#                                            never be evaluated for a misfire situation, and that the scheduler will
#                                            simply try to fire it as soon as it can, and then update the Trigger
#                                            as if it had fired at the proper time.
#                 For cron trigger:
#                     smartPolicy - This is default which will act as `FireAndProceed`
#                     ignoreMisfiresPolicy - If the Trigger misfires, instructs the Scheduler that the trigger will
#                                            never be evaluated for a misfire situation, and that the scheduler will
#                                            simply try to fire it as soon as it can, and then update the trigger as
#                                            if it had fired at the proper time.
#                     doNothing - Instructs the Scheduler If the Trigger misfires, the CronTrigger wants to have
#                                 it's next-fire-time updated to the next time in the schedule after the current time.
#                     fireAndProceed - Instructs the Scheduler If the Trigger misfires, the trigger wants to be fired
#                                      now by Scheduler.
public type MisfireConfiguration record {|
    int thresholdInMillis = 5000;
    InstructionForOneOffTrigger|InstructionForRecurringTrigger|InstructionForCronTrigger|DefaultInstruction instruction = "smartPolicy";
|};

# Possible types of parameters that can be passed into the `InstructionForSingleTrigger`.
public type InstructionForOneOffTrigger "fireNow"|"ignoreMisfiresPoilcy";

# Possible types of parameters that can be passed into the `InstructionForSingleRepeatingTrigger`.
public type InstructionForRecurringTrigger "rescheduleNextWithExistingCount"|"rescheduleNextWithRemainingCount"|
"rescheduleNowWithExistingRepeatCount"|"handlingInstructionNowWithRemainingRepeatCount"|"ignoreMisfiresPoilcy";

# Possible types of parameters that can be passed into the `InstructionForCronTrigger`.
public type InstructionForCronTrigger "doNothing"|"ignoreMisfiresPolicy"|"fireAndProceed";

# Possible types of parameters that can be passed into the `DefaultInstruction`.
public type DefaultInstruction "smartPolicy";
