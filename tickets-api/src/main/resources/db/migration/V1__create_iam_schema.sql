CREATE TABLE roles (
    id   SMALLINT    NOT NULL,
    name VARCHAR(20) NOT NULL,

    CONSTRAINT pk_roles      PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

COMMENT ON TABLE roles IS 'Papeis fixos do sistema. Alterados por codigo, nunca em runtime.';


CREATE TABLE users (
    id            UUID         NOT NULL,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    -- BCrypt gera 60 caracteres; a folga cobre uma futura troca para Argon2.
    password_hash VARCHAR(100) NOT NULL,
    avatar_url    VARCHAR(512),
    -- NULL para ADMIN e AGENT (equipa interna), obrigatorio para REQUESTER.
    customer_id   UUID,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT ck_users_name_filled CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_users_email_shape CHECK (position('@' IN email) > 1)
);

COMMENT ON COLUMN users.customer_id IS
    'Discriminador de tenant. Filtra toda a consulta de tickets feita por um REQUESTER.';


CREATE TABLE user_roles (
    user_id UUID     NOT NULL,
    role_id SMALLINT NOT NULL,

    CONSTRAINT pk_user_roles      PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);


CREATE TABLE refresh_tokens (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_refresh_tokens       PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash  UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_refresh_tokens_dates CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
