package gr.unipi.eshop.cart;

/**
 * Business limits for a cart, in one place. These are compile-time constants (not
 * {@code @ConfigurationProperties}) because they are consumed where only constants are allowed:
 * the {@code @Max} on the request DTOs and the static invariant check in {@link CartEntity}.
 */
final class CartPolicy {

    /**
     * Maximum number of distinct products a cart may hold.
     */
    static final int MAX_ITEMS = 50;

    /**
     * Maximum quantity for a single cart line (enforced on the resulting, merged quantity).
     */
    static final int MAX_QUANTITY_PER_ITEM = 99;

    private CartPolicy() throws IllegalAccessException {
        throw new IllegalAccessException("Utility class");
    }
}
