UPDATE job
SET export_type_specific_parameters = jsonb_set(export_type_specific_parameters, '{vendorEdiOrdersExportConfig}',
  (export_type_specific_parameters->'vendorEdiOrdersExportConfig') || jsonb_build_object(
    'integrationType', 'Ordering',
    'transmissionMethod', 'FTP',
    'fileFormat', 'EDI'
  ), true)
WHERE type = 'EDIFACT_ORDERS_EXPORT' AND export_type_specific_parameters ? 'vendorEdiOrdersExportConfig'
  AND NOT (export_type_specific_parameters->'vendorEdiOrdersExportConfig' ? 'integrationType')
  AND NOT (export_type_specific_parameters->'vendorEdiOrdersExportConfig' ? 'transmissionMethod')
  AND NOT (export_type_specific_parameters->'vendorEdiOrdersExportConfig' ? 'fileFormat');
