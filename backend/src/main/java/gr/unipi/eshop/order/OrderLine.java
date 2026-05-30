package gr.unipi.eshop.order;

import gr.unipi.eshop.cart.CartItem;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderLine(String name, int quantity, BigDecimal price, String currency, BigDecimal subtotal)
        implements Serializable {

    static OrderLine from(CartItem item) {
        return new OrderLine(item.name(), item.quantity(), item.price(), item.currency(), item.subtotal());
    }
}
