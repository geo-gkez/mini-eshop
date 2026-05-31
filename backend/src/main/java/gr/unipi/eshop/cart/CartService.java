package gr.unipi.eshop.cart;

import gr.unipi.eshop.auth.AppUser;
import gr.unipi.eshop.auth.AppUserRepository;
import gr.unipi.eshop.catalog.Product;
import gr.unipi.eshop.catalog.ProductRepository;
import gr.unipi.eshop.shared.LogFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@NullMarked
@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CartResponse view(String username) {
        return findCart(username)
                .map(CartEntity::toResponse)
                .orElseGet(() -> new CartResponse(List.of(), BigDecimal.ZERO));
    }

    public CartResponse addItem(String username, UUID productReference, int quantity) {
        var product = productRepository.findByReference(productReference)
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue(LogFields.Key.EVENT, LogFields.Event.PRODUCT_NOT_FOUND)
                            .addKeyValue(LogFields.Key.REF, productReference)
                            .log("product not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        var cart = getOrCreate(username);

        tryToAddInCar(cart, quantity, product);

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.CART_ITEM_ADDED)
                .addKeyValue(LogFields.Key.PRODUCT, product.getName())
                .addKeyValue(LogFields.Key.QUANTITY, quantity)
                .log("item added product={} quantity={}", product.getName(), quantity);

        return cart.toResponse();
    }

    private static void tryToAddInCar(CartEntity cart, int quantity, Product product) {
        try {
            cart.add(product, quantity);
        } catch (CartFullException e) {
            log.atWarn()
                    .addKeyValue(LogFields.Key.EVENT, LogFields.Event.CART_FULL)
                    .log("cart is full");
            throw e;
        }
    }

    public CartResponse updateItem(String username, UUID reference, int quantity) {
        var cart = getOrCreate(username);

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

    public void removeItem(String username, UUID reference) {
        var cart = getOrCreate(username);
        if (!cart.remove(reference)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.CART_ITEM_REMOVED)
                .addKeyValue(LogFields.Key.REF, reference)
                .log("item removed ref={}", reference);
    }

    public void clearCart(String username) {
        findCart(username).ifPresent(cart -> cart.getItems().clear());
    }

    private Optional<CartEntity> findCart(String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> cartRepository.findByUser_Id(user.getId()));
    }

    private CartEntity getOrCreate(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return cartRepository.findByUser_Id(user.getId())
                .orElseGet(() -> cartRepository.save(new CartEntity(user)));
    }
}
