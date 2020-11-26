## Module Overview

This module provides the functionality to configure and manage Task Listeners and Task Schedulers, which can be used to schedule either one-time or periodically tasks parallely.

#### Task Listeners

A Task `Listener` can be used to create a service listener, which is used to schedule either one-time or periodically tasks under the same scheduling logic(trigger).

Below are the two types of trigger configurations that can be used to configure a Task Listener.

- `IntervalTriggerConfiguration`: It is used to run either one-time or periodically tasks with fixed intervals of time.
- `CronTriggerConfiguration`: It is used to run either one-time or periodically tasks at certain time(s) of day.

##### Schedule interval trigger task by using Listener

The `IntervalTriggerConfiguration` can be used to schedule tasks exactly once at a specific moment in time, or at a specific moment in time followed by repeats at a specific interval.

The following code snippet shows how to create a listener, which registers a task with an initial delay of 3000 milliseconds and is executed every 1000 milliseconds for 10 times.

```ballerina
task:IntervalTriggerConfiguration config = {
    intervalInMillis: 1000,
    initialDelayInMillis: 3000,
    // Number of recurrences will limit the number of times the timer runs.
    noOfRecurrences: 10
};

listener task:Listener simpleTrigger = new(config);

// Creating a service on the `timer` task Listener.
service serv on simpleTrigger {
    // This resource triggers when the timer goes off.
    resource function onTrigger() {
    }
}
```

For an example on schedule a job with a simple trigger by using the `task:Listener`, see the [Task Service Timer Example](https://ballerina.io/swan-lake/learn/by-example/task-service-timer.html).

##### Schedule cron trigger task by using Listener

The `CronTriggerConfiguration` can be used to schedule either one-time or periodically tasks based on calendar-like notions.
  
The following code snippet shows how to create a cron trigger task, which registers a service using a CRON expression to execute the task every second for 10 times.

```ballerina
task:CronTriggerConfiguration config = {
    // This cron expression will schedule the appointment once every second.
    cronExpression: "* * * * * ?",
    // Number of recurrences will limit the number of times the timer runs.
    noOfRecurrences: 10
};

listener task:Listener cronTrigger = new(config);

// Creating a service on the `appointment` task Listener.
service serv on cronTrigger {
    // This resource triggers when the appointment is due.
    resource function onTrigger() {
    }
}
```

For an example on schedule a job with a cron trigger by using the `task:Listener`, see the [Task Service Appointment Example](https://ballerina.io/swan-lake/learn/by-example/task-service-appointment.html).

#### Task Schedulers

A Task `Scheduler` is a advanced scheduler for `Listner`, which can be used to create the scheduler and schedule the multiple tasks with different scheduling logic(trigger). 

below are the three types of configurations that can be used to configure a `task:Scheduler` and schedule the tasks.

- `SchedulerConfiguration`: It is used to configure the `task:Scheduler`.
- `IntervalTriggerConfiguration`: It is used to run either one-time or periodically tasks with fixed intervals of time.
- `CronTriggerConfiguration`: It is used to run either one-time or periodically tasks at certain time(s) of day.

##### Initialize scheduler

The following code snippet shows how to create a `task:Scheduler`.

```ballerina

task:Scheduler scheduler = scheduler(threadCount = 1, threadPriority = 1, thresholdInMillis = 10000);
```

##### Schedule simple trigger tasks by using `Scheduler`

The following code snippet shows how to schedule a simple trigger task.

```ballerina
int count = 0;

// Represents a job
function job() {
    count = count + 1;
}

task:IntervalTriggerConfiguration config = {
        intervalInMillis: 1000,
        initialDelayInMillis: 0,
        noOfRecurrences: 10
};

var taskId = scheduler.scheduleJob(job, config);
```

For an example on schedule a job with a simple trigger by using the `task:Scheduler`, see the [Task Scheduler Timer Example](https://ballerina.io/swan-lake/learn/by-example/task-scheduler-timer.html).

#### Schedule cron trigger tasks by using `Scheduler`

The following code snippet shows how to schedule a cron trigger task.

```ballerina
int count = 0;

// Represents a job
function job() {
    count = count + 1;


task:CronTriggerConfiguration config = {
        appointmentDetails: "* * * * * ?",
        noOfRecurrences: 10
};
var taskId = scheduler.scheduleJob(job, config);
```

For an example on schedule a job with a cron trigger by using the `task:Scheduler`, see the [Task Scheduler Appointment Example](https://ballerina.io/swan-lake/learn/by-example/task-scheduler-appointment.html).
