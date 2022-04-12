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
