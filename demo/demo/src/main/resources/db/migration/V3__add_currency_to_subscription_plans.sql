ALTER TABLE subscription_plans ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD' AFTER price_cents;
