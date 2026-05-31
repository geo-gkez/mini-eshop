package gr.unipi.eshop.cart;

import gr.unipi.eshop.catalog.Product;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Unit test for the MAX_ITEMS=50 rule, which now lives on CartEntity (same package → package-private access).
class CartLimitTest {

    private static Product product(UUID reference) {
        var product = mock(Product.class);
        when(product.getReference()).thenReturn(reference);
        return product;
    }

    private CartEntity cartWithItems(int count) {
        var cart = new CartEntity(null); // add() does not touch the user
        for (int i = 0; i < count; i++) {
            cart.add(product(UUID.randomUUID()), 1);
        }
        return cart;
    }

    @Test
    void add_whenCartFull_throwsCartFullException() {
        var cart = cartWithItems(50);
        var newProduct = product(UUID.randomUUID());

        assertThatThrownBy(() -> cart.add(newProduct, 1))
                .isInstanceOf(CartFullException.class);
    }

    @Test
    void add_whenCartFull_updatingExistingItemStillSucceeds() {
        var existing = UUID.randomUUID();
        var cart = cartWithItems(49);
        cart.add(product(existing), 1);

        assertThatNoException().isThrownBy(() -> cart.add(product(existing), 2));
    }
}
