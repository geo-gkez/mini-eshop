package gr.unipi.eshop.cart;

import gr.unipi.eshop.catalog.ProductRepository;
import gr.unipi.eshop.shared.LogFields;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
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
                .orElseThrow(() -> {
                    log.atWarn().addKeyValue(LogFields.Key.EVENT, LogFields.Event.PRODUCT_NOT_FOUND).addKeyValue(LogFields.Key.REF, productReference).log("product not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        var cart = getOrCreate(session);
        try {
            cart.add(CartItem.of(product, quantity));
        } catch (CartFullException e) {
            log.atWarn().addKeyValue(LogFields.Key.EVENT, LogFields.Event.CART_FULL).log("cart is full");
            throw e;
        }

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.CART_ITEM_ADDED)
                .addKeyValue(LogFields.Key.PRODUCT, product.getName())
                .addKeyValue(LogFields.Key.QUANTITY, quantity)
                .log("item added product={} quantity={}", product.getName(), quantity);

        return cart.toResponse();
    }

    public CartResponse updateItem(HttpSession session, UUID reference, int quantity) {
        var cart = getOrCreate(session);

        if (!cart.update(reference, quantity)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.CART_ITEM_UPDATED)
                .addKeyValue(LogFields.Key.REF, reference)
                .addKeyValue(LogFields.Key.QUANTITY, quantity)
                .log("item updated ref={} quantity={}", reference, quantity);

        return cart.toResponse();
    }

    public void removeItem(HttpSession session, UUID reference) {
        var cart = getOrCreate(session);
        if (!cart.remove(reference)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.CART_ITEM_REMOVED)
                .addKeyValue(LogFields.Key.REF, reference)
                .log("item removed ref={}", reference);
    }

    public void clearCart(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }
}
