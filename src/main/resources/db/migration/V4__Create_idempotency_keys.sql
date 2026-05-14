CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    response_status INT NOT NULL DEFAULT 0,
    response_body TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
