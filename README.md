Ballerina Task Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-task/workflows/Build/badge.svg)](https://github.com/ballerina-platform/module-ballerina-task/actions?query=workflow%3ABuild) 
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-task.svg)](https://github.com/ballerina-platform/module-ballerina-task/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/io.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fio)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This `task` library is one of the standard libraries of <a target="_blank" href="https://ballerina.io/">Ballerina</a> language.

This module provides the functionality to configure and manage periodically task by using Task Listeners and Task Schedulers.

For more information on all the operations supported by the `task:Listener` and `task:Scheduler`, go to [The Task Module](https://ballerina.io/swan-lake/learn/api-docs/ballerina/task/).

For a quick sample on demonstrating the usage see [Ballerina By Example](https://ballerina.io/swan-lake/learn/by-example/).

## Issues and Projects 

Issues and Project are disabled for this repository as this is a part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the module.

## Building from the Source
### Setting Up the Prerequisites

1. Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).
   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
   
   * [OpenJDK](https://adoptopenjdk.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
### Building the Source

Execute the commands below to build from source.

1. To build the library,
        
        ./gradlew clean build

2. To build the module without tests,

        ./gradlew clean build -PskipBallerinaTests

3. To debug the tests,

        ./gradlew clean build -PdebugBallerina=<port>

## Contributing to Ballerina

As an open source project, Ballerina welcomes contributions from the community. To start contributing, read these [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md) for information on how you should go about contributing to our project.

## Code of Conduct

All contributors are encouraged to read [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful Links

* The ballerina-dev@googlegroups.com mailing list is for discussing code changes to the Ballerina project.
* Chat live with us on our [Slack channel](https://ballerina.io/community/slack/).
* Technical questions should be posted on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
