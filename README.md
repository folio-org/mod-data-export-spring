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

### Issue tracker
See project [MODEXPS](https://issues.folio.org/browse/MODEXPS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation
Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at
[dev.folio.org](https://dev.folio.org/)
