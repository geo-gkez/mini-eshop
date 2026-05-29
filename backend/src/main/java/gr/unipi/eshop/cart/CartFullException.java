package gr.unipi.eshop.cart;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CartFullException extends ResponseStatusException {

    public CartFullException() {
        super(HttpStatus.BAD_REQUEST, "Cart is full");
    }
}
