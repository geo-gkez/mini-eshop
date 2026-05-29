package gr.unipi.eshop.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItem> items, BigDecimal total) {
}
