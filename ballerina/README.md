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

#### Frequency-based job execution

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

### Listener-based job execution

This approach enables you to schedule and execute recurring and one-time jobs as services with configurable trigger settings on the attached listener. This allows users to deploy a scheduled task as a service without the need of a thread sleep.

The following code snippet shows how to schedule a job by using the listener based approach.

```ballerina
listener task:Listener taskListener = new (
    trigger = {
        interval: 2,
        maxCount: 5
    }
);

service "job-1" on taskListener {
    private int i = 1;

    isolated function execute() returns error? {
        lock {
            io:println("Counter: ", self.i);
            self.i += 1;
        }
    }
}
```

You can enhance job reliability by adding retry configurations for failing job executions in task listeners.

```ballerina
listener task:Listener taskListener = new (
    trigger = {
        interval: 20,
        maxCount: 5
    },
    retryConfig: {
        maxAttempts: 5,
        retryInterval: 1,
        maxInterval: 5
    }
);
```

### Task coordination

Task coordination support is specifically designed for distributed systems where high availability and fault tolerance are essential requirements. The coordination mechanism ensures that when tasks are running across multiple nodes, only one node remains active while others stay on standby. If the active node fails or becomes unavailable, one of the standby nodes automatically takes over, maintaining continuous system availability and preventing service interruptions.

The system utilizes an RDBMS-based coordination mechanism to handle availability across multiple nodes, significantly improving the reliability and uptime of distributed applications in production environments.
The task coordination system follows a warm backup approach with the following characteristics:

- Multiple nodes run identical program logic on separate task instances
- One node is designated as the token bearer and actively executes the program logic
- Other nodes act as watchdogs by continuously monitoring the status of the token bearer node
- If the active node fails or becomes unresponsive, one of the candidate nodes automatically takes over without manual intervention

The following code snippet demonstrates how to implement task coordination across multiple nodes.

```ballerina
listener task:Listener taskListener = new (
  trigger = {
    interval,
    maxCount
  }, 
  warmBackupConfig = {
    databaseConfig,
    livenessCheckInterval,
    taskId, // must be unique for each node
    groupId,
    heartbeatFrequency
  }
);

service "job-1" on taskListener {
  private int i = 1;

  isolated function execute() {
    // Add your business logic here
    // This will only execute on the active node
  }
}
```
