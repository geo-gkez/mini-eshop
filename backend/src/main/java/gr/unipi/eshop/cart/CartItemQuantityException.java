package gr.unipi.eshop.cart;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a single cart line's quantity would exceed {@link CartPolicy#MAX_QUANTITY_PER_ITEM}.
 * The per-request {@code @Max(999)} bean-validation cap only bounds one call; without this the
 * merge path ({@code addQuantity}) accumulates unbounded across repeated add requests.
 */
public class CartItemQuantityException extends ResponseStatusException {

    public CartItemQuantityException() {
        super(HttpStatus.BAD_REQUEST,
                "Quantity exceeds the maximum of " + CartPolicy.MAX_QUANTITY_PER_ITEM + " allowed per item");
    }
}
