# Change Log
This file contains all the notable changes done to the Ballerina Task package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.11.2] - 2026-05-29

### Fixed
- [Fix JDBC connection leak in the warm-backup coordination layer](https://github.com/ballerina-platform/ballerina-library/issues/8806)

## [2.11.1] - 2026-01-09

### Fixed
- [Fix start and end time configs in the listener](https://github.com/ballerina-platform/module-ballerina-task/pull/543)
- [Fix `maxInterval` in the retry configuration to be optional](https://github.com/ballerina-platform/module-ballerina-task/pull/541)

## [2.11.0] - 2025-07-25

### Added
- [Add retry support for listeners](https://github.com/wso2-enterprise/internal-support-ballerina/issues/1043)
- [Add task coordination and listener sections to the spec](https://github.com/ballerina-platform/module-ballerina-task/pull/540)
- [Add task coordination example](https://github.com/ballerina-platform/module-ballerina-task/pull/538)
- [Add listener support in docs](https://github.com/ballerina-platform/module-ballerina-task/pull/536)

## [2.10.0] - 2025-05-16

### Added
- [Add listener support](https://github.com/ballerina-platform/module-ballerina-task/pull/534)
- [Add task coordination support](https://github.com/ballerina-platform/module-ballerina-task/pull/535)

## [2.8.1] - 2025-04-29

### Fixed
- Fix supporting restarted nodes in task coordination

## [2.8.0] - 2025-04-29

### Changed
- [Update coordination config to `WarmBackupConfig` and add `taskId`/`groupId` to uniquely identify tasks](https://github.com/ballerina-platform/module-ballerina-task/pull/528)
- Improve the token acquisition API and fix DB queries to support group IDs

## [2.7.0] - 2025-03-12

### Changed
- Update dependencies to Ballerina 2201.12.0 (Swan Lake Update 12)

## [2.6.0] - 2025-02-07

### Changed
- [Migrate to Java 21](https://github.com/ballerina-platform/module-ballerina-task/pull/507)
- [Make Java utility classes proper utility classes](https://github.com/ballerina-platform/ballerina-standard-library/issues/5052)

## [2.5.0] - 2023-09-15

### Changed
- [Migrate to Java 17](https://github.com/ballerina-platform/module-ballerina-task/pull/484)
- Upgrade quartz dependency
- [Update the implementation for disabling quartz logs](https://github.com/ballerina-platform/ballerina-standard-library/issues/4282)

## [2.4.0] - 2023-06-30

### Changed
- [Mark GraalVM native-image compatibility](https://github.com/ballerina-platform/module-ballerina-task/pull/474)

## [2.3.2] - 2023-04-22

### Fixed
- [Fix quartz log suppression across all execution paths](https://github.com/ballerina-platform/ballerina-standard-library/issues/4282)

## [2.3.1] - 2023-03-29

### Changed
- [Enable non-quartz logs](https://github.com/ballerina-platform/ballerina-standard-library/issues/4282)

## [2.3.0] - 2022-09-29

### Added
- [Add GraalVM native-image build tool support](https://github.com/ballerina-platform/ballerina-standard-library/issues/3315)

### Changed
- [API docs updated](https://github.com/ballerina-platform/ballerina-standard-library/issues/3463)

## [2.0.0-beta.1] - 2021-06-02

### Changed
- Update the quartz library version to 2.3.2

## [2.0.0-alpha5] - 2021-03-19

### Added
- [Add more test cases](https://github.com/ballerina-platform/ballerina-standard-library/issues/1217)

## [2.0.0-alpha4] - 2021-02-20

### Changed
- [Revamp the package](https://github.com/ballerina-platform/ballerina-standard-library/issues/62)
