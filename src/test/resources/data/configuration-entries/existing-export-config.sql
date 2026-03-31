INSERT INTO diku_mod_data_export_spring.export_config
  (id, config_name, type, tenant, export_type_specific_parameters,
   schedule_frequency, schedule_period, schedule_time, week_days,
   created_date, created_by, updated_date, updated_by)
VALUES
  ('a1111111-1111-1111-1111-111111111111', 'export_config_parameters', 'BURSAR_FEES_FINES', 'diku',
   '{"bursarFeeFines": {"daysOutstanding": 10}}',
   1, 'DAY', '12:00:00.000Z', null,
   '2025-01-01 00:00:00', '625dd2b6-b6f2-4f77-90fe-68954b26ee3c',
   '2025-01-01 00:00:00', '625dd2b6-b6f2-4f77-90fe-68954b26ee3c');

