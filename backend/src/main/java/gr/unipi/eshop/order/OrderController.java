package gr.unipi.eshop.order;

import gr.unipi.eshop.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@Validated
@RestController
@AllArgsConstructor
@RequestMapping(value = "/api/order", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderController {

    private final OrderService orderService;

    @PostMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderReview> submit(@RequestBody @Valid ShippingAddress address, @CurrentUser UserDetails user) {
        return ResponseEntity.ok(orderService.submit(user.getUsername(), address));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@CurrentUser UserDetails user) {
        orderService.confirm(user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
