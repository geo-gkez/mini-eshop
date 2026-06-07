package gr.unipi.eshop.cart;

import gr.unipi.eshop.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@NullMarked
@Validated
@RestController
@AllArgsConstructor
@RequestMapping(value = "/api/cart", produces = MediaType.APPLICATION_JSON_VALUE)
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> view(@CurrentUser UserDetails user) {
        return ResponseEntity.ok(cartService.view(user.getUsername()));
    }

    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CartResponse> addItem(@RequestBody @Valid AddItemRequest addItemRequest,
                                                @CurrentUser UserDetails user) {
        return ResponseEntity
                .ok(cartService.addItem(user.getUsername(), addItemRequest.productReference(), addItemRequest.quantity()));
    }

    @PatchMapping(value = "/items/{reference}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CartResponse> updateItem(@PathVariable UUID reference,
                                                   @RequestBody @Valid UpdateItemRequest updateItemRequest,
                                                   @CurrentUser UserDetails user) {
        return ResponseEntity.ok(cartService.updateItem(user.getUsername(), reference, updateItemRequest.quantity()));
    }

    @DeleteMapping("/items/{reference}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID reference, @CurrentUser UserDetails user) {
        cartService.removeItem(user.getUsername(), reference);

        return ResponseEntity.noContent().build();
    }
}
