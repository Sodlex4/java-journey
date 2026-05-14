CREATE TABLE IF NOT EXISTS rate_limits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip_address VARCHAR(45) NOT NULL,
    window_start BIGINT NOT NULL,
    request_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ip_window (ip_address, window_start)
);
