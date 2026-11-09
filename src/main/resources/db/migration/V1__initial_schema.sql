CREATE TABLE organizations (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(160) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE memberships (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  role VARCHAR(32) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_membership_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uq_membership_org_user UNIQUE (organization_id, user_id),
  INDEX ix_membership_user (user_id)
);

CREATE TABLE refresh_tokens (
  token_hash VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  organization_id VARCHAR(36) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  revoked_at TIMESTAMP(6) NULL,
  replaced_by_hash VARCHAR(64) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_refresh_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  INDEX ix_refresh_user (user_id),
  INDEX ix_refresh_expiry (expires_at)
);

CREATE TABLE contracts (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  title VARCHAR(240) NOT NULL,
  counterparty VARCHAR(240) NULL,
  contract_type VARCHAR(100) NULL,
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_contract_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_contract_creator FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX ix_contract_org_created (organization_id, created_at),
  INDEX ix_contract_org_status (organization_id, status)
);

CREATE TABLE documents (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  contract_id VARCHAR(36) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  media_type VARCHAR(160) NOT NULL,
  byte_size BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_document_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_document_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
  CONSTRAINT uq_document_org_checksum UNIQUE (organization_id, sha256),
  INDEX ix_document_org_contract (organization_id, contract_id)
);

CREATE TABLE processing_jobs (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  document_id VARCHAR(36) NOT NULL,
  state VARCHAR(32) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_job_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_job_document FOREIGN KEY (document_id) REFERENCES documents(id),
  INDEX ix_job_org_state (organization_id, state)
);

CREATE TABLE document_chunks (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  document_id VARCHAR(36) NOT NULL,
  chunk_index INT NOT NULL,
  page_number INT NULL,
  content MEDIUMTEXT NOT NULL,
  vector_id VARCHAR(100) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_chunk_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES documents(id),
  CONSTRAINT uq_chunk_document_index UNIQUE (document_id, chunk_index),
  INDEX ix_chunk_org_doc (organization_id, document_id)
);

CREATE TABLE extracted_clauses (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  contract_id VARCHAR(36) NOT NULL,
  document_id VARCHAR(36) NOT NULL,
  clause_type VARCHAR(100) NOT NULL,
  value_json JSON NOT NULL,
  page_number INT NULL,
  evidence_text TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_clause_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_clause_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
  CONSTRAINT fk_clause_document FOREIGN KEY (document_id) REFERENCES documents(id),
  INDEX ix_clause_org_contract (organization_id, contract_id)
);

CREATE TABLE risks (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  contract_id VARCHAR(36) NOT NULL,
  document_id VARCHAR(36) NOT NULL,
  category VARCHAR(100) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  description TEXT NOT NULL,
  evidence_text TEXT NOT NULL,
  page_number INT NULL,
  review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  reviewed_by VARCHAR(36) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_risk_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_risk_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
  CONSTRAINT fk_risk_document FOREIGN KEY (document_id) REFERENCES documents(id),
  INDEX ix_risk_org_contract (organization_id, contract_id)
);

CREATE TABLE conversations (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  title VARCHAR(240) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_conversation_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX ix_conversation_org_user (organization_id, user_id)
);

CREATE TABLE conversation_messages (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  conversation_id VARCHAR(36) NOT NULL,
  role VARCHAR(32) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  citations_json JSON NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_message_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
  INDEX ix_message_org_conversation (organization_id, conversation_id, created_at)
);

CREATE TABLE obligations (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  contract_id VARCHAR(36) NOT NULL,
  document_id VARCHAR(36) NOT NULL,
  description TEXT NOT NULL,
  due_date DATE NULL,
  status VARCHAR(32) NOT NULL,
  source_page INT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_obligation_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_obligation_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
  CONSTRAINT fk_obligation_document FOREIGN KEY (document_id) REFERENCES documents(id),
  INDEX ix_obligation_org_due (organization_id, due_date)
);

CREATE TABLE notifications (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  obligation_id VARCHAR(36) NULL,
  reminder_offset INT NULL,
  kind VARCHAR(80) NOT NULL,
  payload_json JSON NOT NULL,
  read_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_notification_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_notification_obligation FOREIGN KEY (obligation_id) REFERENCES obligations(id),
  CONSTRAINT uq_notification_reminder UNIQUE (user_id, obligation_id, reminder_offset),
  INDEX ix_notification_org_user (organization_id, user_id, created_at)
);

CREATE TABLE audit_events (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  actor_user_id VARCHAR(36) NULL,
  action VARCHAR(120) NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  resource_id VARCHAR(36) NULL,
  correlation_id VARCHAR(100) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_audit_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  INDEX ix_audit_org_time (organization_id, created_at),
  INDEX ix_audit_org_resource (organization_id, resource_type, resource_id)
);

CREATE TABLE ai_usage (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  provider VARCHAR(80) NOT NULL,
  model VARCHAR(160) NOT NULL,
  operation VARCHAR(100) NOT NULL,
  input_tokens BIGINT NULL,
  output_tokens BIGINT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_ai_usage_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX ix_ai_usage_org_time (organization_id, created_at)
);

CREATE TABLE outbox_events (
  id VARCHAR(36) PRIMARY KEY,
  aggregate_type VARCHAR(100) NOT NULL,
  aggregate_id VARCHAR(36) NOT NULL,
  organization_id VARCHAR(36) NOT NULL,
  event_type VARCHAR(120) NOT NULL,
  payload_json JSON NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  published_at TIMESTAMP(6) NULL,
  INDEX ix_outbox_pending (published_at, created_at)
);
