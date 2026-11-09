ALTER TABLE obligations
  ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  ADD COLUMN source_key VARCHAR(64) NULL,
  ADD COLUMN source_evidence TEXT NULL;

CREATE INDEX ix_obligation_org_status_due
  ON obligations (organization_id, status, due_date);
CREATE UNIQUE INDEX uq_obligation_document_source
  ON obligations (document_id, source_key);

CREATE TABLE email_outbox (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
  notification_id VARCHAR(36) NOT NULL UNIQUE,
  recipient VARCHAR(254) NOT NULL,
  subject VARCHAR(240) NOT NULL,
  html_content MEDIUMTEXT NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  sent_at TIMESTAMP(6) NULL,
  CONSTRAINT fk_email_outbox_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_email_outbox_notification FOREIGN KEY (notification_id) REFERENCES notifications(id),
  INDEX ix_email_outbox_pending (sent_at, created_at)
);
