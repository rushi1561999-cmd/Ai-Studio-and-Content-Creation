ALTER TABLE payments ADD COLUMN provider_payment_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN updated_at DATETIME(6);
CREATE UNIQUE INDEX uk_payment_provider_external ON payments (provider, external_id);
