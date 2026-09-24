-- Owner of the payment, used to answer GET-by-id only to that user (or ADMIN). Rows created before this
-- migration have no owner and are therefore visible to ADMIN / internal calls only.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS user_id UUID;
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments (user_id);
