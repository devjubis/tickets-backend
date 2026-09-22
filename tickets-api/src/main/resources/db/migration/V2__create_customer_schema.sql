CREATE TABLE customers (
    id              UUID         NOT NULL,
    trade_name      VARCHAR(160) NOT NULL,
    legal_name      VARCHAR(200),
    -- CNPJ normalizado: apenas digitos, imposto pelo value object DocumentNumber.
    document_number VARCHAR(14)  NOT NULL,
    email           VARCHAR(180),
    phone           VARCHAR(20),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_customers             PRIMARY KEY (id),
    CONSTRAINT ck_customers_trade_name  CHECK (length(btrim(trade_name)) > 0),
    CONSTRAINT ck_customers_document    CHECK (document_number ~ '^[0-9]{11,14}$')
    -- A unicidade de document_number
);

CREATE TABLE customer_addresses (
    id              UUID         NOT NULL,
    customer_id     UUID         NOT NULL,
    street          VARCHAR(200) NOT NULL,
    number          VARCHAR(20),
    complement      VARCHAR(100),
    district        VARCHAR(100),
    city            VARCHAR(120) NOT NULL,
    state           CHAR(2)      NOT NULL,
    postal_code     VARCHAR(8)   NOT NULL,
    primary_address BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_customer_addresses          PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id)
        REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT ck_customer_addresses_state    CHECK (state ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_customer_addresses_postal   CHECK (postal_code ~ '^[0-9]{8}$')
);

-- Garante no maximo uma morada principal por cliente. Um indice unico parcial
-- faz isto sem trigger e sem logica na aplicacao.
CREATE UNIQUE INDEX uk_customer_addresses_primary
    ON customer_addresses (customer_id)
    WHERE primary_address;

ALTER TABLE users
    ADD CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers (id);
