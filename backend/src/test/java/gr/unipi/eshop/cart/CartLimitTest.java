package gr.unipi.eshop.cart;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartLimitTest {

    private static CartItem item(UUID reference) {
        return new CartItem(reference, "Product", BigDecimal.ONE, "EUR", 1, BigDecimal.ONE);
    }

    private Cart cartWithItems(int count) {
        var cart = new Cart();
        for (int i = 0; i < count; i++) {
            cart.add(item(UUID.randomUUID()));
        }
        return cart;
    }

    @Test
    void add_whenCartFull_throwsCartFullException() {
        var cart = cartWithItems(50);
        var newItem = item(UUID.randomUUID());

        assertThatThrownBy(() -> cart.add(newItem))
                .isInstanceOf(CartFullException.class);
    }

    @Test
    void add_whenCartFull_updatingExistingItemStillSucceeds() {
        var existing = UUID.randomUUID();
        var cart = cartWithItems(49);
        cart.add(item(existing));

        assertThatNoException().isThrownBy(() -> cart.add(item(existing)));
    }
}
