## 2022-06-08 v1.2.4
[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.2.3...v1.2.4)

### Bug fixes
* [MODEXPS-117](https://issues.folio.org/browse/MODEXPS-117) Release 1.2.4 fixing ZipException on 64-bit systems (Kiwi HF#4)

No module code changes. The base Docker container has been fixed.

## 2022-04-14 v1.2.3
The primary focus of this release to improve security level of application by updating used libraries versions

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.2.2...v1.2.3)

* [MODEXPS-61](https://issues.folio.org/browse/MODEXPS-61) Adding perms.users.assign.immutable
* [MODEXPS-67](https://issues.folio.org/browse/MODEXPS-67) Require circulation-logs
* spring-boot-starter-parent 2.5.12, log4j 2.17.2: fixing CVE-2022-22965, CVE-2022-21724, CVE-2022-26520,
  CVE-2020-36518, CVE-2022-23181, CVE-2022-22950, CVE-2021-45105, CVE-2021-44832

## 2022-03-04 v1.2.2
The primary focus of this release is fixing several tenants usage errors for data export process

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.2.1...v1.2.2)

### Bug fixes
* [MODEXPS-76](https://issues.folio.org/browse/MODEXPS-76) - Backport to Kiwi HF #2 - Fix several tenants usage errors for data export process

## 2021-12-20 v1.2.1
The primary focus of this release is fixing log4j vulnerability

[Full Changelog](https://github.com/folio-org/mod-data-export-spring/compare/v1.2.0...v1.2.1)

### Bug fixes
* [MODEXPS-52](https://issues.folio.org/browse/MODEXPS-52) - Kiwi R3 2021 - Log4j vulnerability verification and correction


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
