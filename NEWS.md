## 2023-10-12 v3.0.0

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v2.0.2...v3.0.0)

### Technical tasks
* [MODEXPS-224](https://issues.folio.org/browse/MODEXPS-224) Enable API-related GitHub Workflows, replace those Jenkins stages
* [MODEXPS-218](https://issues.folio.org/browse/MODEXPS-218) Remove feature flag and old edifact scheduling code
* [MODEXPS-211](https://issues.folio.org/browse/MODEXPS-211) Migrate to folio-spring-support v7.0.0
* [MODEXPS-199](https://issues.folio.org/browse/MODEXPS-199) Update mod-data-export-spring to folio-spring-base v6.1.0
* [MODEXPS-74](https://issues.folio.org/browse/MODEXPS-74) Logging improvement

### Stories
* [MODEXPS-233](https://issues.folio.org/browse/MODEXPS-233) Allow Tenant Collection Topics
* [MODEXPS-221](https://issues.folio.org/browse/MODEXPS-221) Deletion Of Old Jobs from Job Table
* [MODEXPS-217](https://issues.folio.org/browse/MODEXPS-217) Migration of existing jobs to quartz on module upgrade for Bursar
* [MODEXPS-216](https://issues.folio.org/browse/MODEXPS-216) Edifact Scheduling final updates: tuning configuration, docs update
* [MODEXPS-213](https://issues.folio.org/browse/MODEXPS-213) Reimplementation of Bursar Scheduling to use Quartz
* [MODEXPS-210](https://issues.folio.org/browse/MODEXPS-210) Implement logic for tenant disabling for quartz scheduling
* [MODEXPS-209](https://issues.folio.org/browse/MODEXPS-209) Edifact Orders Export quartz scheduling testing
* [MODEXPS-205](https://issues.folio.org/browse/MODEXPS-205) Migration of existing jobs to quartz on module upgrade
* [MODEXPS-204](https://issues.folio.org/browse/MODEXPS-204) Add possibility to run jobs without 'module enable' for tenant call
* [MODEXPS-203](https://issues.folio.org/browse/MODEXPS-203) Reimplementation of Edifact Scheduling to use Quartz
* [MODEXPS-185](https://issues.folio.org/browse/MODEXPS-185) Prepare initial setup for introducing Quartz as a scheduler in mod-data-export-spring
* [MODEXPS-167](https://issues.folio.org/browse/MODEXPS-167) Implement refresh token rotation

### Bug fixes
* [MODEXPS-226](https://issues.folio.org/browse/MODEXPS-226) Invalid system user credentials usage
* [MODEXPS-191](https://issues.folio.org/browse/MODEXPS-191) Sorting by export method resets search
* [MODEXPS-175](https://issues.folio.org/browse/MODEXPS-175) Weekly exports are ran according to both the new and the previous (not active) schedule

## 2023-07-20 v2.0.2

## 2023-02-24 v2.0.0

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.5.3...v2.0.0)

### Technical tasks
* [MODEXPS-187](https://issues.folio.org/browse/MODEXPS-187) Authority control: add new export type
* [MODEXPS-181](https://issues.folio.org/browse/MODEXPS-181) mockserver 5.14.0 for arm64 support
* [MODEXPS-177](https://issues.folio.org/browse/MODEXPS-177) Update the module to Spring boot v3.0.0 and identify issues.
* [MODEXPS-163](https://issues.folio.org/browse/MODEXPS-163) Logging improvement - Configuration

## 2022-10-20 v1.5.0

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.4.0...v1.5.0)

### Stories
* [MODEXPS-157](https://issues.folio.org/browse/MODEXPS-157) Implement Re-send EDIFACT export logic
* [MODEXPS-141](https://issues.folio.org/browse/MODEXPS-141) Bulk edit: holdings locations - Update submodule
* [MODEXPS-140](https://issues.folio.org/browse/MODEXPS-140) Order export job should not be rerun for deleted integration methods
* [MODEXPS-63](https://issues.folio.org/browse/MODEXPS-63) Implement download logic from MINIO for Download export file

### Technical tasks
* [MODEXPS-159](https://issues.folio.org/browse/MODEXPS-159) mod-data-export-spring: spring upgrade
* [MODEXPS-130](https://issues.folio.org/browse/MODEXPS-130) Supports users interface version 15.3, 16.0

## 2022-07-08 v1.4.0

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.3.0...v1.4.0)

### Technical tasks
* [MODEXPS-114](https://issues.folio.org/browse/MODEXPS-114) Migrate to new progress json schema
* [MODEXPS-79](https://issues.folio.org/browse/MODEXPS-79) Update folio-export-common schemas
* [MODEXPS-73](https://issues.folio.org/browse/MODEXPS-73) mod-data-export-spring: folio-spring-base v4.1.0 update

### Stories
* [MODEXPS-107](https://issues.folio.org/browse/MODEXPS-107) Add migration test InstallUpgradeIT
* [MODEXPS-106](https://issues.folio.org/browse/MODEXPS-106) Update folio-export-common schemas
* [MODEXPS-103](https://issues.folio.org/browse/MODEXPS-103)  Improve error handling if provided data from EDIFACT export configuration has incorrect format
* [MODEXPS-94](https://issues.folio.org/browse/MODEXPS-94) Export eHoldings: ability to create export jobs
* [MODEXPS-39](https://issues.folio.org/browse/MODEXPS-39) Improve scheduling logic
* [MODEXPS-21](https://issues.folio.org/browse/MODEXPS-21) Remove gen_random_uuid(), it fails in pgpool native replication

### Bug fixes
* [MODEXPS-124](https://issues.folio.org/browse/MODEXPS-124) Bursar scheduling - Morning Glory work

## 2022-06-08 v1.3.5

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.3.4...v1.3.5)

### Bug fixes
* [MODEXPS-116](https://issues.folio.org/browse/MODEXPS-116) Release 1.3.5 fixing ZipException on 64-bit systems (Lotus HF#1)

No module code changes. The base Docker container has been fixed.

## 2022-04-18 v1.3.4

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.3.3...v1.3.4)

### Bug fixes
* [MODEXPS-95](https://issues.folio.org/browse/MODEXPS-95) Authtoken required

## 2022-04-15 v1.3.3

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.3.2...v1.3.3)

### Bug fixes
* [MODEXPS-91](https://issues.folio.org/browse/MODEXPS-91) Scheduled Job is not saved in the Lotus Bugfest Database

## 2022-04-10 v1.3.2

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.3.1...v1.3.2)

### Bug fixes
* [MODEXPS-90](https://issues.folio.org/browse/MODEXPS-90) Fix issue with permissions for EDIFACT manual orders export


## 2022-04-10 v1.3.1

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.3.0...v1.3.1)

### Bug fixes
* [MODEXPS-70](https://issues.folio.org/browse/MODEXPS-70) Fix issue with EDIFACT Job hour scheduling after mod-data-export-spring restart or update
* [MODEXPS-82](https://issues.folio.org/browse/MODEXPS-82) Fix issue with permissions for EDIFACT orders export

## 2022-03-03 v1.3.0

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.2.1...v1.3.0)

### Technical tasks
* [MODEXPS-66](https://issues.folio.org/browse/MODEXPS-66) mod-data-export-spring: folio-spring-base v3 update

### Stories
* [MODEXPS-37](https://issues.folio.org/browse/MODEXPS-37) Add a new export type
* [MODEXPS-40](https://issues.folio.org/browse/MODEXPS-40) Create schema to support EDI order export functionality
* [MODEXPS-41](https://issues.folio.org/browse/MODEXPS-41) Refactor existing approach to store configuration for Schedulers based on "export type"
* [MODEXPS-43](https://issues.folio.org/browse/MODEXPS-43) Add new export type
* [MODEXPS-47](https://issues.folio.org/browse/MODEXPS-47) Reuse common schemas
* [MODEXPS-48](https://issues.folio.org/browse/MODEXPS-48) Remove expired files
* [MODEXPS-54](https://issues.folio.org/browse/MODEXPS-54) Implement scheduling of the EDIFACT orders export
* [MODEXPS-61](https://issues.folio.org/browse/MODEXPS-61) User creation fails due to lack of permissions

### Bug fixes
* [MODEXPS-59](https://issues.folio.org/browse/MODEXPS-59) Fix several tenants usage errors for data export process
* [MODEXPS-68](https://issues.folio.org/browse/MODEXPS-68) Schedule date not saving correctly
* [MODEXPS-67](https://issues.folio.org/browse/MODEXPS-67) Missing require on circulation-logs

## 2021-11-10 v1.2.0

### Stories

* [MODEXPS-26](https://issues.folio.org/browse/MODEXPS-26) Scheduling export job doesn't work anymore
* [MODEXPS-29](https://issues.folio.org/browse/MODEXPS-29) mod-data-export-spring: folio-spring-base v2 update
* [MODEXPS-34](https://issues.folio.org/browse/MODEXPS-34) Rename JobRepository to avoid conflicts with springframework

## 2021-08-06 v1.1.3
* [MODEXPS-24](https://issues.folio.org/browse/MODEXPS-24) Kafka topic created with incorrect ENV and tenantId combination

## 2021-07-05 v1.1.2
 * [MODEXPS-23](https://issues.folio.org/browse/MODEXPS-23) MODEXPS-23 Enable mutiowner mapping feature

## 2021-07-05 v1.1.1
 * [MODEXPS-19](https://issues.folio.org/browse/MODEXPS-19) Update topics to use ENV configuration in naming
 * [MODEXPS-22](https://issues.folio.org/browse/MODEXPS-22) Data export job status, start/end time not updated

## 2021-06-18 v1.1.0
 * No changes since last release.

## 2021-05-14 v1.0.5
 * [MODEXPS-12](https://issues.folio.org/browse/MODEXPS-12) Add standard health check endpoint

## 2021-05-06 v1.0.4
 * [MODEXPS-17](https://issues.folio.org/browse/MODEXPS-17) Username and password expressed in plain text in module logs

## 2021-04-19 v1.0.3
 * [MODEXPS-15](https://issues.folio.org/browse/MODEXPS-15) Kafka connection does not start without tenant registration

## 2021-04-12 v1.0.2
 * [MODEXPS-9](https://issues.folio.org/browse/MODEXPS-9) Precreated user can't login because has no permissions
 * [MODEXPS-10](https://issues.folio.org/browse/MODEXPS-10) Enhance fees/fines bursar report settings
 * [MODEXPS-13](https://issues.folio.org/browse/MODEXPS-13) Kafka topics aren't created automatically

## 2021-03-18 v1.0.1
 * First module release

## 2021-01-29 v0.0.1
 * Initial module setup
