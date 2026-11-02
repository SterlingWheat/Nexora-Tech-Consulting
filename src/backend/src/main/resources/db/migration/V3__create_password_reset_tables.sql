CREATE TABLE IF NOT EXISTS password_reset_rate_limit (
    email_fingerprint VARCHAR(64) PRIMARY KEY,
    ultima_solicitud TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    id_usuario INTEGER REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    email_fingerprint VARCHAR(64) NOT NULL,
    token_hash VARCHAR(64) UNIQUE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_uso TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_usuario
    ON password_reset_token(id_usuario);
