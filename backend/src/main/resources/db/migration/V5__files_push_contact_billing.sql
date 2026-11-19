-- FleetPulse V5: file storage index, push devices, public contact requests, webhook idempotency
CREATE TABLE files (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  uploaded_by BIGINT NOT NULL,
  purpose VARCHAR(30) NOT NULL,              -- POD, RECEIPT, DOCUMENT, LICENCE, LOGO, OTHER
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_key VARCHAR(300) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_files_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_files_user FOREIGN KEY (uploaded_by) REFERENCES users(id),
  INDEX ix_files_company (company_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE device_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  token VARCHAR(300) NOT NULL UNIQUE,
  platform VARCHAR(10) NOT NULL,             -- ANDROID, IOS
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_dt_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_dt_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX ix_dt_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE contact_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  kind VARCHAR(10) NOT NULL,                 -- DEMO or CONTACT
  name VARCHAR(160) NOT NULL,
  email VARCHAR(190) NOT NULL,
  mobile VARCHAR(24),
  company_name VARCHAR(160),
  message TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  ip_address VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX ix_contact_status (status, created_at)
) ENGINE=InnoDB;

CREATE TABLE processed_webhook_events (
  event_id VARCHAR(120) PRIMARY KEY,
  provider VARCHAR(20) NOT NULL,
  processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
