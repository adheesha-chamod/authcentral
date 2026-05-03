CREATE TABLE audit_logs (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    action     VARCHAR(100) NOT NULL,
    username   VARCHAR(100),
    user_type  VARCHAR(20),
    ip_address VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
