# Overview

This application demonstrates how to schedule one-time or recurrence jobs to get closed GitHub issues by using `task` and `http` packages.

## Prerequisite

Update all the GitHub related configuration in the `Config.toml`

## Run the example
 
First, clone this repository, and then run the following commands to run this example in your local machine.
 
    $ cd examples/closed-issue-counter
    $ bal run
 

### Sample requests

Run the following curl request to schedule or manage jobs.

#### Schedule one time job
    curl -v -X POST http://localhost:9092/scheduler/oneTimeJob --data "START_TIME" 
    Eg: curl -v -X POST http://localhost:9092/scheduler/oneTimeJob --data "2021-08-26T01:55:00.520+05:30[Asia/Colombo]"

#### Schedule recurrence job forever
    curl -v -X POST http://localhost:9092/scheduler/recurJob/[INTERVAL]
    Eg: curl -v -X POST http://localhost:9092/scheduler/recurJob/100 --data "{\"startTime\":\"\"}" 
    Here, A startTime may have value or be empty. If you add value, that should in this format[2021-08-26T01:55:00.520+05:30[Asia/Colombo]].

#### Schedule recurrence job with trigger count
    curl -v -X POST http://localhost:9092/scheduler/recurJob/[COUNT]/[INTERVAL]
    Eg: curl -v -X POST http://localhost:9092/scheduler/recurJob/5/100 --data "{\"startTime\":\"\"}" 
    Here, A startTime may have value or be empty. If you add value, that should in this format[2021-08-26T01:55:00.520+05:30[Asia/Colombo]].

#### Unschedule a particular job
    curl -v -X POST http://localhost:9092/scheduler/unscheduleJob/[JOB_ID]
    Eg: curl -v -X POST http://localhost:9092/scheduler/unscheduleJob/9945

#### Pause a particular job
    curl -v -X POST http://localhost:9092/scheduler/pauseJob/[JOB_ID]
    Eg: curl -v -X POST http://localhost:9092/scheduler/pauseJob/9945

#### Resume a particular job
    curl -v -X POST http://localhost:9092/scheduler/resumeJob/[JOB_ID]
    Eg: curl -v -X POST http://localhost:9092/scheduler/resumeJob/9945

#### Pause all running jobs
    curl -v -X POST http://localhost:9092/scheduler/resumeAllJobs/[JOB_ID]
    Eg: curl -v -X POST http://localhost:9092/scheduler/resumeAllJobs/9945

#### Resume all jobs
    curl -v -X POST http://localhost:9092/scheduler/pauseAllJobs/[JOB_ID]
    Eg: curl -v -X POST http://localhost:9092/scheduler/pauseAllJobs/9945

#### Get all running jobs
    curl -v -X GET http://localhost:9092/scheduler/getRunningJobs 
    Eg: curl -v -X GET http://localhost:9092/scheduler/getRunningJobs

#### Shutdown the service
    curl -v -X POST http://localhost:9092/scheduler/shutdown
    Eg: curl -v -X POST http://localhost:9092/scheduler/shutdown
