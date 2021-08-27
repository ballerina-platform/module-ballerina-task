# Overview

This application demonstrates how to schedule one-time or recurring jobs to retrieve a list of closed GitHub issues by using the `task` and `http` packages.

## Prerequisite

Update all the GitHub-related configurations in the `Config.toml` file.

## Run the example
 
First, clone this repository, and then run the following commands to run this example in your local machine.
 
    $ cd examples/closed-issue-counter
    $ bal run


### Send sample requests

Run the following cURL request to schedule or manage jobs.

#### Schedule a one time job
    curl -v -X POST http://localhost:9092/scheduler/oneTimeJob --data "START_TIME" 
    E.g., curl -v -X POST http://localhost:9092/scheduler/oneTimeJob --data "2021-08-26T01:55:00.520+05:30[Asia/Colombo]"

#### Schedule a recurrence job for forever
    curl -v -X POST http://localhost:9092/scheduler/recurJob/[INTERVAL]
    E.g., curl -v -X POST http://localhost:9092/scheduler/recurJob/100 --data "{\"startTime\":\"\"}" 
    Here, A startTime may have a value or be empty. If you add a value, that should be in this format[2021-08-26T01:55:00.520+05:30[Asia/Colombo]].

#### Schedule recurrence job with trigger count
    curl -v -X POST http://localhost:9092/scheduler/recurJob/[COUNT]/[INTERVAL]
    E.g., curl -v -X POST http://localhost:9092/scheduler/recurJob/5/100 --data "{\"startTime\":\"\"}" 
    Here, A startTime may have a value or be empty. If you add a value, that should be in this format[2021-08-26T01:55:00.520+05:30[Asia/Colombo]].

#### Unschedule a particular job
    curl -v -X POST http://localhost:9092/scheduler/unscheduleJob/[JOB_ID]
    E.g., curl -v -X POST http://localhost:9092/scheduler/unscheduleJob/9945

#### Pause a particular job
    curl -v -X POST http://localhost:9092/scheduler/pauseJob/[JOB_ID]
    E.g., curl -v -X POST http://localhost:9092/scheduler/pauseJob/9945

#### Resume a particular job
    curl -v -X POST http://localhost:9092/scheduler/resumeJob/[JOB_ID]
    E.g., curl -v -X POST http://localhost:9092/scheduler/resumeJob/9945

#### Pause all running jobs
    curl -v -X POST http://localhost:9092/scheduler/resumeAllJobs/[JOB_ID]
    E.g., curl -v -X POST http://localhost:9092/scheduler/resumeAllJobs/9945

#### Resume all jobs
    curl -v -X POST http://localhost:9092/scheduler/pauseAllJobs/[JOB_ID]
    E.g., curl -v -X POST http://localhost:9092/scheduler/pauseAllJobs/9945

#### Get all running jobs
    curl -v -X GET http://localhost:9092/scheduler/getRunningJobs 
    E.g., curl -v -X GET http://localhost:9092/scheduler/getRunningJobs

#### Shutdown the service
    curl -v -X POST http://localhost:9092/scheduler/shutdown
    E.g., curl -v -X POST http://localhost:9092/scheduler/shutdown
