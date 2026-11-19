-- FleetPulse V3: Super Admin / platform tables
CREATE TABLE platform_invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  plan_id BIGINT NULL,
  invoice_number VARCHAR(40) NOT NULL UNIQUE,
  period_start DATE NULL,
  period_end DATE NULL,
  subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,
  tax DECIMAL(14,2) NOT NULL DEFAULT 0,
  total DECIMAL(14,2) NOT NULL DEFAULT 0,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',   -- OPEN, PAID, FAILED, VOID, REFUNDED
  issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_at TIMESTAMP NULL,
  paid_at TIMESTAMP NULL,
  CONSTRAINT fk_pinv_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_pinv_plan FOREIGN KEY (plan_id) REFERENCES plans(id),
  INDEX ix_pinv_company (company_id, issued_at)
) ENGINE=InnoDB;

CREATE TABLE platform_payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  platform_invoice_id BIGINT NULL,
  amount DECIMAL(14,2) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  status VARCHAR(20) NOT NULL,                  -- SUCCEEDED, FAILED, REFUND_REQUESTED, REFUNDED
  gateway VARCHAR(40),
  gateway_ref VARCHAR(120),
  failure_reason VARCHAR(300),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ppay_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_ppay_invoice FOREIGN KEY (platform_invoice_id) REFERENCES platform_invoices(id),
  INDEX ix_ppay_status (status, created_at)
) ENGINE=InnoDB;

CREATE TABLE platform_settings (
  setting_key VARCHAR(80) PRIMARY KEY,
  setting_value TEXT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE feature_flags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  flag_key VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(300),
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE announcements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  starts_at TIMESTAMP NULL,
  ends_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE ai_models (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  model_id VARCHAR(160) NOT NULL UNIQUE,
  display_name VARCHAR(160) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT FALSE,
  input_cost_per_1k DECIMAL(10,6) NOT NULL DEFAULT 0,
  output_cost_per_1k DECIMAL(10,6) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO platform_settings(setting_key, setting_value) VALUES
 ('maintenance_mode','false'),
 ('maintenance_message','FleetPulse is undergoing scheduled maintenance. Please try again shortly.'),
 ('general.platform_name','FleetPulse'),
 ('general.support_email',''),
 ('email.from_name','FleetPulse'),
 ('sms.provider','twilio'),
 ('payment.gateway',''),
 ('branding.primary_color','#FF8733'),
 ('ai.enabled','false'),
 ('ai.default_model',''),
 ('ai.rate_limit_per_minute','20');

INSERT INTO feature_flags(flag_key, description, enabled) VALUES
 ('ai_assistant','NVIDIA NIM fleet assistant',FALSE),
 ('whatsapp_notifications','WhatsApp Business notifications',FALSE),
 ('customer_tracking_links','Public tracking links',TRUE);
