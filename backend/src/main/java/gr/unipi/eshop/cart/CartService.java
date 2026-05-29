package gr.unipi.eshop.cart;

import gr.unipi.eshop.catalog.ProductRepository;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@NullMarked
@Service
@AllArgsConstructor
public class CartService {

    private static final String SESSION_KEY = "cart";

    private final ProductRepository productRepository;

    public CartResponse view(HttpSession session) {
        return getOrCreate(session).toResponse();
    }

    private Cart getOrCreate(HttpSession session) {
        if (session.getAttribute(SESSION_KEY) instanceof Cart cart) {
            return cart;
        }

        var cart = new Cart();
        session.setAttribute(SESSION_KEY, cart);

        return cart;
    }

    public CartResponse addItem(HttpSession session, UUID productReference, int quantity) {
        var product = productRepository.findByReference(productReference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var cart = getOrCreate(session);
        cart.add(CartItem.of(product, quantity));

        return cart.toResponse();
    }

    public CartResponse updateItem(HttpSession session, UUID reference, int quantity) {
        var cart = getOrCreate(session);

        if (!cart.update(reference, quantity)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return cart.toResponse();
    }

    public void removeItem(HttpSession session, UUID reference) {
        var cart = getOrCreate(session);
        if (!cart.remove(reference)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
