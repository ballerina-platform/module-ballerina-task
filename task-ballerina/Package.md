## Package Overview

This package provides the functionality to schedule and manages the execution of Ballerina jobs, either just once or periodically.

### Jobs and Scheduling

Every scheduled job in Ballerina is represented by a Job object. You need to create jobs class with custom logic to execute when the task is triggered.

Task package has the following two scheduling systems to schedule the job:

- One-time Job Execution
- Frequency-based Job Execution 

#### One-time Job Execution

This API provide to schedule a job at a specified date.

The following code snippet shows how to schedule the one-time job.


```ballerina
class Job {

    *task:Job;
    string msg;

    public function execute() {
        io:println(msg);
    }

    isolated function init(string msg) {
        self.msg = msg;
    }
}

time:ZoneOffset zoneOffset = {
    hours: 5,
    minutes: 30,
    seconds: <decimal>0.0
};
time:Civil time = {
    year: 2021,
    month: 4,
    day: 13,
    hour: 4,
    minute: 50,
    second: 50.52,
    timeAbbrev: "Asia/Colombo",
    utcOffset: zoneOffset
};

task:Error|task:JobId result = scheduleOneTimeJob(new Job("Hi"), time);
```
For an example on the usage of the `scheduleOneTimeJob`, see the [Task One-time Job Execution Example](https://ballerina.io/learn/by-example/task-one-time-job-execution.html).

##### Frequency-based Job Execution

This API provides to schedule jobs on a specific interval, either just once or periodically with optional start time, end times, and maximum count.

The following code snippet shows how to how to schedule the recurrence job by using this API

```ballerina
class Job {

    *task:Job;
    string msg;

    public function execute() {
        io:println(msg);
    }

    isolated function init(string msg) {
        self.msg = msg;
    }
}

time:ZoneOffset zoneOffset = {
    hours: 5,
    minutes: 30
};
time:Civil time = {
    year: 2021,
    month: 3,
    day: 31,
    hour: 4,
    minute: 50,
    second: 50.52,
    timeAbbrev: "Asia/Colombo",
    utcOffset: zoneOffset
};

task:Error|task:JobId result = scheduleJobRecurByFrequency(new Job("Hi"), 2.5, maxCount = 10, startTime = time);
```
For an example on the usage of the `scheduleJobRecurByFrequency`, see the [Task Frequency Job Execution Example](https://ballerina.io/learn/by-example/task-frequency-job-execution.html).
