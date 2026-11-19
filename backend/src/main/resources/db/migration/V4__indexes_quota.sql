-- GPS monthly quota checks and usage reporting
CREATE INDEX ix_gps_company_received ON gps_locations (company_id, received_at);
