package gr.unipi.eshop.order;

import gr.unipi.eshop.catalog.Product;

import java.math.BigDecimal;

public record OrderLine(String name, int quantity, BigDecimal price, String currency, BigDecimal subtotal) {

    static OrderLine from(Product product, int quantity) {
        return new OrderLine(
                product.getName(),
                quantity,
                product.getPrice(),
                product.getCurrency(),
                product.getPrice().multiply(BigDecimal.valueOf(quantity))
        );
    }
}
