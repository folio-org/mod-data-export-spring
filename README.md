# mod-data-export-spring

Copyright (C) 2021 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file [LICENSE](LICENSE) for more information.

## Introduction
API for Data Export Spring module.

## Environment variables:

| Name                          | Default value             | Description                                                       |
| :-----------------------------| :------------------------:|:------------------------------------------------------------------|
| DB_HOST                       | postgres                  | Postgres hostname                                                 |
| DB_PORT                       | 5432                      | Postgres port                                                     |
| DB_USERNAME                   | folio_admin               | Postgres username                                                 |
| DB_PASSWORD                   | -                         | Postgres username password                                        |
| DB_DATABASE                   | okapi_modules             | Postgres database name                                            |
| KAFKA_HOST                    | kafka                     | Kafka broker hostname                                             |
| KAFKA_PORT                    | 9092                      | Kafka broker port                                                 |
| OKAPI_URL                     | http://okapi:9130         | Okapi url                                                         |
| ENV                           | folio                     | Logical name of the deployment, must be set if Kafka/Elasticsearch are shared for environments, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed|


## Additional information
Data Export Spring API provides the following URLs:

|  Method | URL| Permissions  | Description  | 
|---|---|---|---|
| GET  | /data-export-spring/jobs/        | data-export.job.collection.get    | Gets jobs                                |
| GET  | /data-export-spring/jobs/{id}    | data-export.job.item.get          | Gets a job by the job ID                 |
| POST | /data-export-spring/jobs/        | data-export.job.item.post         | Upserts a job                            |
| GET  | /data-export-spring/configs/     | data-export.config.collection.get | Get a list of data export configurations |
| PUT  | /data-export-spring/configs/{id} | data-export.config.item.put       | Change an export configuration           |
| POST | /data-export-spring/configs/     | data-export.config.item.post      | Add an export configuration              |

More detail can be found on Data Export Spring wiki-page: [WIKI Data Export Spring](https://wiki.folio.org/pages/viewpage.action?pageId=52134948).

### Required Permissions
Institutional users should be granted the following permissions in order to use this Data Export Spring API:
- data-export.config.all
- data-export.job.all

### Deployment information
![](https://img.shields.io/static/v1?label=&message=!WARNING&color=orange)
 Only ONE instance should be running until the issues described below are fixed: 

1. If more than one instance of the module is running, then the same export tasks will be launched by all instances.
As a result, information will be duplicated.

More details:
[Prevents execution of the same scheduled export task from another node](https://issues.folio.org/browse/MODEXPS-75)

2. If instance of the module will be restarted by Kubernetes or manually and no need to register modules again,
because version is not changed. As a result, mandatory information for scheduling (Okapi headers, system user, tenant information)
will not be stored in memory in a FolioExecutionContext.

More details:
[Export scheduling doesn't support work in the cluster and after restarting docker container](https://issues.folio.org/browse/MODEXPS-81)

#### Short overview
Before running scheduled task(job) there is check, that module is registered for the Okapi tenant.

Tenant information need to define DB schema for storing information about Job and etc.

In the post tenant API controller specific user (`data-export-system-user`) is created for running scheduled export tasks. Permissions are defined in `src/main/resources/permissions/system-user-permissions.csv`.

Also Okapi headers, system user, tenant information are stored in memory in a FolioExecutionContext.

### Issue tracker
See project [MODEXPS](https://issues.folio.org/browse/MODEXPS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation
Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at
[dev.folio.org](https://dev.folio.org/)
