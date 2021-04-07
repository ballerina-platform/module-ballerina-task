## Package Overview

This package provides the functionality to schedule a Ballerina job and manages the execution of Ballerina jobs either once or periodically.

### Jobs and Scheduling

Every scheduled job in Ballerina is represented by a Job object. You need to create a jobs class with custom logic to execute it when the task is triggered.

The Task package has the following two scheduling systems to schedule the job:

- One-time Job Execution
- Frequency-based Job Execution 

#### One-time Job Execution

This API provides the functionality to schedule a job at a specified date.

The following code snippet shows how to schedule a one-time job.


```ballerina
class Job {

    *task:Job;
    string msg;

    public function execute() {
        io:println(self.msg);
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

task:Error|task:JobId result = task:scheduleOneTimeJob(new Job("Hi"), time);
```
For an example on the usage of the `scheduleOneTimeJob`, see the [Task One-time Job Execution Example](https://ballerina.io/learn/by-example/task-one-time-job-execution.html).

##### Frequency-based Job Execution

This API provides the functionality to schedule jobs on a specific interval either once or periodically with an optional start time, end time, and maximum count.

The following code snippet shows how to schedule a recurrence job by using this API.

```ballerina
class Job {

    *task:Job;
    string msg;

    public function execute() {
        io:println(self.msg);
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

task:Error|task:JobId result = task:scheduleJobRecurByFrequency(new Job("Hi"), 2.5, maxCount = 10, startTime = time);
```
For an example on the usage of the `scheduleJobRecurByFrequency`, see the [Task Frequency Job Execution Example](https://ballerina.io/learn/by-example/task-frequency-job-execution.html).
