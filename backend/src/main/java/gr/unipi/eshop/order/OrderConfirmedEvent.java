package gr.unipi.eshop.order;

/**
 * Published by {@link OrderService#confirm} once an order is marked CONFIRMED. Carries only the id so
 * the listener re-reads a fresh, managed entity in its own transaction. Handled AFTER_COMMIT so the
 * SMTP round-trip never runs inside (and holds open) the checkout transaction.
 */
record OrderConfirmedEvent(Long orderId) {}
