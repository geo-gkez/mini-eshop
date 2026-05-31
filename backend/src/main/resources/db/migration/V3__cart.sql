CREATE TABLE carts (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL UNIQUE REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT    NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id BIGINT    NOT NULL REFERENCES products (id),
    quantity   INT       NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

-- At most one pending order per user (the checkout-in-progress snapshot).
CREATE UNIQUE INDEX uq_orders_user_pending ON orders (user_id) WHERE status = 'PENDING';
