## Overview

This module provides APIs to schedule a Ballerina job either once or periodically and to manage the execution of those jobs.

### Jobs and scheduling

Every scheduling job in Ballerina needs to be represented by a `Job` object. Therefore, a `job` class with your custom logic needs to be created to execute it when the task is triggered.

The `task` package has the following two scheduling systems to schedule the job:

- One-time job execution
- Frequency-based job execution 

#### One-time job execution

This API provides the functionality to schedule a job at a specified time.

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

task:JobId result = check task:scheduleOneTimeJob(new Job("Hi"), time);
```

##### Frequency-based job execution

This API provides the functionality to schedule jobs on a specific interval either once or periodically by configuring the configuration such as start time, end time, and maximum count.

The following code snippet shows how to schedule a recurring job by using this API.

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

task:JobId result = check task:scheduleJobRecurByFrequency(new Job("Hi"), 2.5, maxCount = 10, startTime = time);
```

For information on the operations, which you can perform with the task module, see the below **Functions**.
