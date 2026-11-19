-- FleetPulse V1: platform, tenancy, identity, subscriptions, audit
CREATE TABLE plans (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(500),
  monthly_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  annual_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  trial_days INT NOT NULL DEFAULT 14,
  vehicle_limit INT NOT NULL,
  user_limit INT NOT NULL,
  ai_monthly_quota INT NOT NULL DEFAULT 0,
  gps_monthly_quota BIGINT NOT NULL DEFAULT 0,
  features_json JSON NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE companies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  email VARCHAR(190) NOT NULL,
  mobile VARCHAR(24) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',  -- TRIAL, ACTIVE, SUSPENDED, EXPIRED, CANCELLED
  email_verified_at TIMESTAMP NULL,
  mobile_verified_at TIMESTAMP NULL,
  logo_url VARCHAR(500),
  address VARCHAR(500),
  country VARCHAR(80),
  timezone VARCHAR(60) NOT NULL DEFAULT 'UTC',
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_company_email UNIQUE (email),
  CONSTRAINT uq_company_mobile UNIQUE (mobile)
) ENGINE=InnoDB;
CREATE INDEX ix_companies_status ON companies(status);

CREATE TABLE roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE permissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  module VARCHAR(40) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  email VARCHAR(190) NOT NULL,
  mobile VARCHAR(24),
  password_hash VARCHAR(100) NOT NULL,
  role_code VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, DISABLED
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP NULL,
  last_login_at TIMESTAMP NULL,
  token_version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_users_role FOREIGN KEY (role_code) REFERENCES roles(code),
  CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB;
CREATE INDEX ix_users_company ON users(company_id, role_code);

CREATE TABLE refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject_type VARCHAR(10) NOT NULL,   -- USER or ADMIN
  subject_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  user_agent VARCHAR(300),
  ip_address VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX ix_rt_subject ON refresh_tokens(subject_type, subject_id);

CREATE TABLE pending_registrations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  public_id CHAR(36) NOT NULL UNIQUE,
  company_name VARCHAR(160) NOT NULL,
  company_email VARCHAR(190) NOT NULL,
  mobile VARCHAR(24) NOT NULL,
  admin_name VARCHAR(160) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  plan_code VARCHAR(40),
  email_otp_hash CHAR(64) NOT NULL,
  mobile_otp_hash CHAR(64) NOT NULL,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  mobile_verified BOOLEAN NOT NULL DEFAULT FALSE,
  email_attempts INT NOT NULL DEFAULT 0,
  mobile_attempts INT NOT NULL DEFAULT 0,
  last_sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX ix_pending_email ON pending_registrations(company_email);
CREATE INDEX ix_pending_mobile ON pending_registrations(mobile);

CREATE TABLE subscriptions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,             -- TRIALING, ACTIVE, PAST_DUE, CANCELLED, EXPIRED
  billing_cycle VARCHAR(10) NOT NULL DEFAULT 'MONTHLY',
  trial_ends_at TIMESTAMP NULL,
  current_period_start TIMESTAMP NOT NULL,
  current_period_end TIMESTAMP NOT NULL,
  cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
  cancelled_at TIMESTAMP NULL,
  gateway_customer_ref VARCHAR(120),
  gateway_subscription_ref VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_sub_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
) ENGINE=InnoDB;
CREATE INDEX ix_sub_company ON subscriptions(company_id, status);

CREATE TABLE platform_admins (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  display_name VARCHAR(120) NOT NULL DEFAULT 'Fayzo Super Admin',
  password_hash VARCHAR(100) NOT NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP NULL,
  token_version INT NOT NULL DEFAULT 0,
  last_login_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE login_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject_type VARCHAR(10) NOT NULL,
  subject_id BIGINT NULL,
  identifier VARCHAR(190),
  success BOOLEAN NOT NULL,
  reason VARCHAR(80),
  ip_address VARCHAR(64),
  user_agent VARCHAR(300),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX ix_login_activity ON login_activity(subject_type, created_at);

CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NULL,
  actor_type VARCHAR(10) NOT NULL,
  actor_id BIGINT NULL,
  action VARCHAR(80) NOT NULL,
  entity_type VARCHAR(60),
  entity_id VARCHAR(60),
  details_json JSON NULL,
  ip_address VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_company FOREIGN KEY (company_id) REFERENCES companies(id)
) ENGINE=InnoDB;
CREATE INDEX ix_audit_company ON audit_logs(company_id, created_at);

-- Seed roles
INSERT INTO roles (code, name) VALUES
 ('COMPANY_ADMIN','Company Admin'),('OPERATIONS_MANAGER','Operations Manager'),
 ('FLEET_MANAGER','Fleet Manager'),('ACCOUNTANT','Accountant'),('HR_MANAGER','HR Manager'),
 ('DRIVER','Driver'),('WAREHOUSE_MANAGER','Warehouse Manager'),('CUSTOMER_SUPPORT','Customer Support');

-- Seed default plans (editable by Super Admin; backend is the source of truth for pricing)
INSERT INTO plans (code,name,description,monthly_price,annual_price,trial_days,vehicle_limit,user_limit,ai_monthly_quota,gps_monthly_quota,sort_order) VALUES
 ('STARTER','Starter','For small fleets getting started',29.00,290.00,14,10,5,50,100000,1),
 ('PROFESSIONAL','Professional','For growing logistics operations',99.00,990.00,14,50,25,500,1000000,2),
 ('ENTERPRISE','Enterprise','For large fleets and multi-branch operators',299.00,2990.00,14,1000,500,5000,20000000,3);
