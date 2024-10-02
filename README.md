Ballerina Task Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-task/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-task/actions/workflows/build-timestamped-master.yml)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-task/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-task)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-task/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-task/actions/workflows/trivy-scan.yml)  
  [![GraalVM Check](https://github.com/ballerina-platform/module-ballerina-task/actions/workflows/build-with-bal-test-graalvm.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-task/actions/workflows/build-with-bal-test-graalvm.yml)  
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-task.svg)](https://github.com/ballerina-platform/module-ballerina-task/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/task.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Ftask)

This library provides APIs to schedule a Ballerina job either once or periodically and to manage the execution of those jobs.

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

For a quick sample on demonstrating the usage, see [Ballerina By Example](https://ballerina.io/learn/by-example/).

## Issues and projects 

Issues and Project are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the package.

## Build from the source
### Set up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 21 (from one of the following locations).
   * [Oracle](https://www.oracle.com/java/technologies/downloads/)
   
   * [OpenJDK](https://adoptium.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
### Build the source

Execute the commands below to build from the source.

1. To build the library:
        
        ./gradlew clean build
        
2. To run the tests:

        ./gradlew clean test
        
3. To build the package without tests:

        ./gradlew clean build -x test

4. To run a group of tests:

        ./gradlew clean test -Pgroups=<test_group_names>

5. To debug package implementation:

        ./gradlew clean build -Pdebug=<port>
        
6. To debug the package with Ballerina language:

        ./gradlew clean build -PbalJavaDebug=<port>
        
7. Publish ZIP artifact to the local `.m2` repository:

        ./gradlew clean build publishToMavenLocal

8. Publish the generated artifacts to the local Ballerina central repository:
   
        ./gradlew clean build -PpublishToLocalCentral=true

9. Publish the generated artifacts to the Ballerina central repository:

        ./gradlew clean build -PpublishToCentral=true


## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community. To start contributing, read these [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md) for information on how you should go about contributing to our project.

## Code of conduct

All contributors are encouraged to read [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`task` library](https://lib.ballerina.io/ballerina/task/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us on our [Discord server](https://discord.gg/ballerinalang).
* Technical questions should be posted on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
