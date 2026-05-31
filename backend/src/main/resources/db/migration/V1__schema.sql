CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE products (
    id          BIGSERIAL      PRIMARY KEY,
    reference   UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name        VARCHAR(200)   NOT NULL,
    description TEXT           NOT NULL,
    price       NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    currency    VARCHAR(3)     NOT NULL,
    attributes  JSONB          NOT NULL DEFAULT '{}'
);

CREATE TABLE orders (
    id          BIGSERIAL      PRIMARY KEY,
    reference   UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id     BIGINT         NOT NULL REFERENCES users (id),
    street      TEXT           NOT NULL,
    city        TEXT           NOT NULL,
    postal_code TEXT           NOT NULL,
    country     TEXT           NOT NULL,
    total       NUMERIC(10, 2) NOT NULL CHECK (total > 0),
    status      VARCHAR(20)    NOT NULL DEFAULT 'CONFIRMED',
    email_sent  BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE TABLE order_lines (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id),
    product_name VARCHAR(200)   NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(10, 2) NOT NULL CHECK (unit_price > 0),
    currency     VARCHAR(3)     NOT NULL,
    subtotal     NUMERIC(10, 2) NOT NULL CHECK (subtotal > 0)
);
