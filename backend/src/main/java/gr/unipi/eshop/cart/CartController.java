package gr.unipi.eshop.cart;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CartResponse> view(HttpSession session) {
        return ResponseEntity.ok(cartService.view(session));
    }

    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CartResponse> addItem(@RequestBody @Valid AddItemRequest addItemRequest,
                                                HttpSession session) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.addItem(session, addItemRequest.productReference(), addItemRequest.quantity()));
    }

    @PatchMapping(value = "/items/{reference}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CartResponse> updateItem(@PathVariable UUID reference,
                                                   @RequestBody @Valid UpdateItemRequest updateItemRequest,
                                                   HttpSession session) {
        return ResponseEntity.ok(cartService.updateItem(session, reference, updateItemRequest.quantity()));
    }

    @DeleteMapping("/items/{reference}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID reference, HttpSession session) {
        cartService.removeItem(session, reference);

        return ResponseEntity.noContent().build();
    }
}
