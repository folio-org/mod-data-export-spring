INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('12ae5d0f-1525-44a1-a361-0bc9b88e8179', '000112', 'test-desc', 'data-export-system-user', true,
        'BURSAR_FEES_FINES', '{
    "bursarFeeFines": {
      "patronGroups": [
        "3684a786-6671-4268-8ed0-9db82ebca60b"
      ],
      "daysOutstanding": 10
    }
  }', 'SUCCESSFUL', '[
    "somefile"
  ]', '2021-03-16 09:29:54.250000', '2021-03-16 09:30:09.831000', '2021-03-16 09:29:54.170000',
        null, 'data-export-system-user', '2021-03-16 09:30:09.968000', null,
        'data-export-system-user', 'Fees & Fines Bursar Report', null, 'COMPLETED', '{
    "running": false,
    "exitCode": "COMPLETED"
  }');
INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('35ae5d0f-1525-42a1-a361-1bc9b88e8180', '000100', 'test-desc', 'data-export-system-user', true,
        'EDIFACT_ORDERS_EXPORT',
        '{}',
        'SUCCESSFUL', '[]', '2021-03-16 09:29:54.250000', '2021-03-16 09:30:09.831000', '2021-03-16 09:29:54.170000',
        null, 'data-export-system-user', '2021-03-16 09:30:09.968000', null,
        'data-export-system-user', 'Fees & Fines Bursar Report', null, 'COMPLETED', '{
    "running": false,
    "exitCode": "COMPLETED"
  }');

INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('42ae5d0f-6425-82a1-a361-1bc9b88e8172', '000101', 'test-desc', 'data-export-system-user', true,
        'EDIFACT_ORDERS_EXPORT',
        '{}',
        'SUCCESSFUL', '["http:/test-url/"]', '2021-03-16 09:29:54.250000', '2021-03-16 09:30:09.831000', '2021-03-16 09:29:54.170000',
        null, 'data-export-system-user', '2021-03-16 09:30:09.968000', null,
        'data-export-system-user', 'Fees & Fines Bursar Report', null, 'COMPLETED', '{
    "running": false,
    "exitCode": "COMPLETED"
  }');
INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('88c2801e-922d-44a1-8b78-9f0f30de376b', '000113', null, 'data-export-system-user', true,
        'BURSAR_FEES_FINES', '{
    "bursarFeeFines": {
      "patronGroups": [
        "3684a786-6671-4268-8ed0-9db82ebca60b"
      ],
      "daysOutstanding": 10
    }
  }', 'FAILED', '[
    "somefile"
  ]', '2021-03-16 09:41:10.040000', '2021-03-16 09:43:54.031000', '2021-03-16 09:41:09.708000',
        null, 'data-export-system-user', '2021-03-16 09:43:54.208000', null,
        'data-export-system-user', 'Fees & Fines Bursar Report', 'Read timed out', 'FAILED', '{
    "running": false,
    "exitCode": "FAILED"
  }');
INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('9c13fd5a-4ab7-4563-a294-e6354ba74e95', '000114', null, 'diku_admin', false,
        'BURSAR_FEES_FINES', '{
    "bursarFeeFines": {
      "patronGroups": [
        "3684a786-6671-4268-8ed0-9db82ebca60b"
      ],
      "daysOutstanding": 10
    }
  }', 'SCHEDULED', null, null, null, '2021-03-17 01:55:47.104000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', '2021-03-17 01:55:47.104000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', 'Fees & Fines Bursar Report', null,
        'UNKNOWN', '{
    "running": true,
    "exitCode": "UNKNOWN"
  }');
INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('9d72fb44-eef7-4b9c-9bd9-f191feec6255', '000115', null, 'diku_admin', false,
        'BURSAR_FEES_FINES', '{
    "bursarFeeFines": {
      "patronGroups": [
        "3684a786-6671-4268-8ed0-9db82ebca60b"
      ],
      "daysOutstanding": 10
    }
  }', 'SCHEDULED', '["test file"]', null, null, '2021-03-17 01:55:49.542000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', '2021-03-17 01:55:49.542000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', 'Fees & Fines Bursar Report', null,
        'UNKNOWN', '{
    "running": true,
    "exitCode": "UNKNOWN"
  }');
INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('e4feb2f6-d3b6-4a05-977c-45f312c7247e', '000116', null, 'diku_admin', false,
        'BURSAR_FEES_FINES', '{
    "bursarFeeFines": {
      "patronGroups": [
        "3684a786-6671-4268-8ed0-9db82ebca60b"
      ],
      "daysOutstanding": 10
    }
  }', 'SCHEDULED', null, null, null, '2021-03-17 01:58:48.323000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', '2021-03-17 01:58:48.323000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', 'Fees & Fines Bursar Report', null,
        'UNKNOWN', '{
    "running": true,
    "exitCode": "UNKNOWN"
  }');

INSERT INTO diku_mod_data_export_spring.job (id, name, description, source, is_system_source, type,
                                             export_type_specific_parameters, status, files,
                                             start_time, end_time, created_date, created_by_user_id,
                                             created_by_username, updated_date, updated_by_user_id,
                                             updated_by_username, output_format, error_details,
                                             batch_status, exit_status)
VALUES ('e4feb2f6-d3b6-4a05-977c-45f312c7248e', '000117', null, 'diku_admin', false,
        'EDIFACT_ORDERS_EXPORT', '{
        "vendorEdiOrdersExportConfig": {
                                          "exportConfigId": "f18d8154-a02f-4414-9c52-c4f9083f1c32",
                                          "vendorId": "11fb627a-cdf1-11e8-a8d5-f2801f1b9fd1",
                                          "configName": "test1",
                                          "ediConfig": {
                                              "accountNoList": [
                                                  "1234"
                                              ],
                                              "libEdiType": "31B/US-SAN",
                                              "vendorEdiType": "31B/US-SAN",
                                              "sendAccountNumber": false,
                                              "supportOrder": false,
                                              "supportInvoice": false
                                          },
                                          "ediFtp": {
                                              "ftpConnMode": "Active",
                                              "ftpFormat": "SFTP",
                                              "ftpMode": "ASCII"
                                          },
                                          "isDefaultConfig": false
                                        }
         }',
        'SCHEDULED', null, null, null, '2021-03-17 01:58:48.323000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', '2021-03-17 01:58:48.323000',
        '1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f', 'diku_admin', 'EDIFACT orders export (EDI)', null,
        'UNKNOWN', '{
    "running": true,
    "exitCode": "UNKNOWN"
  }');
