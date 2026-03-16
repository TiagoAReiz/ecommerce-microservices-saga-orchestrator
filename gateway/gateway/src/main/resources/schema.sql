CREATE TABLE IF NOT EXISTS saga_states (
    id UUID PRIMARY KEY,
    saga_type VARCHAR(50) NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    user_id UUID,
    order_id UUID,
    payload TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_saga_states_status ON saga_states(status);
CREATE INDEX IF NOT EXISTS idx_saga_states_updated_at ON saga_states(updated_at);
