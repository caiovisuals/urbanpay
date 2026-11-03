-- Módulo identity: usuários e refresh tokens.
-- Nunca guardamos senha/PIN em texto: apenas hashes Argon2id.

CREATE TABLE users (
    id                  UUID         PRIMARY KEY,
    full_name           VARCHAR(150) NOT NULL,
    email               VARCHAR(254) NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    cpf                 VARCHAR(11)  NOT NULL,
    birth_date          DATE         NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    pin_hash            VARCHAR(255) NOT NULL,
    pin_failed_attempts INTEGER      NOT NULL DEFAULT 0,
    pin_locked_until    TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_cpf   UNIQUE (cpf),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);

-- Refresh tokens opacos com rotação. Guardamos só o SHA-256 do token.
-- Todos os tokens gerados a partir de um mesmo login compartilham a family_id:
-- se um token já usado for reapresentado (roubo), a família inteira é revogada.
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id   UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    -- Preenchido quando o token foi trocado por um novo (rotação), e não revogado por logout/roubo.
    rotated_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
