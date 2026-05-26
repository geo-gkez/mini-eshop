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
