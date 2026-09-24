-- Owner of the delivery, used to answer GET-by-id only to that user (or ADMIN). Rows created before this
-- migration have no owner and are therefore visible to ADMIN / internal calls only.
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS user_id UUID;
CREATE INDEX IF NOT EXISTS idx_deliveries_user_id ON deliveries (user_id);
