package gr.unipi.eshop.cart;

import gr.unipi.eshop.catalog.Product;

import java.math.BigDecimal;
import java.util.UUID;

record CartItem(
        UUID productReference,
        String name,
        BigDecimal price,
        String currency,
        int quantity,
        BigDecimal subtotal) {

    static CartItem of(Product product, int quantity) {
        return new CartItem(
                product.getReference(),
                product.getName(),
                product.getPrice(),
                product.getCurrency(),
                quantity,
                product.getPrice().multiply(BigDecimal.valueOf(quantity))
        );
    }

    CartItem withQuantity(int newQuantity) {
        return new CartItem(
                productReference,
                name,
                price,
                currency,
                newQuantity,
                price.multiply(BigDecimal.valueOf(newQuantity))
        );
    }
}
