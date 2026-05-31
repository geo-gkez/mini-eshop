package gr.unipi.eshop.order;

import gr.unipi.eshop.auth.AppUser;
import gr.unipi.eshop.auth.AppUserRepository;
import gr.unipi.eshop.cart.CartService;
import gr.unipi.eshop.catalog.ProductRepository;
import gr.unipi.eshop.shared.LogFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Slf4j
@NullMarked
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private static final String PENDING = "PENDING";

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final OrderRepository orderRepository;
    private final JavaMailSender mailSender;
    private final OrderProperties orderProperties;

    public OrderReview submit(String username, ShippingAddress address) {
        var cart = cartService.view(username);
        if (cart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        // Re-read current prices from DB — price at submit time, not at add-to-cart time.
        // If a product was removed from the catalogue between add-to-cart and submit, fail fast.
        var lines = cart.items().stream().map(item -> {
            var product = productRepository.findByReference(item.productReference())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_CONTENT,
                            "Product no longer available: " + item.name()
                    ));
            return OrderLine.from(product, item.quantity());
        }).toList();

        var total = lines.stream()
                .map(OrderLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var review = new OrderReview(address, lines, total);
        var user = requireUser(username);

        // One pending order per user — replace any earlier checkout-in-progress.
        orderRepository.findByUser_IdAndStatus(user.getId(), PENDING).ifPresent(existing -> {
            orderRepository.delete(existing);
            orderRepository.flush();
        });
        orderRepository.save(OrderEntity.pending(review, user));

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.ORDER_SUBMITTED)
                .addKeyValue("items", lines.size())
                .addKeyValue("total", total)
                .log("order submitted items={} total={}", lines.size(), total);

        return review;
    }

    public void confirm(String username) {
        var user = requireUser(username);
        var order = orderRepository.findByUser_IdAndStatus(user.getId(), PENDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending order"));

        order.markConfirmed();

        try {
            sendEmail(order);
            order.markEmailSent();
        } catch (Exception e) {
            // Order stays CONFIRMED in the DB; ops can find unsent ones: WHERE email_sent = false
            log.atError()
                    .addKeyValue(LogFields.Key.EVENT, "ORDER_EMAIL_FAILED")
                    .addKeyValue(LogFields.Key.USER, username)
                    .addKeyValue("orderReference", order.getReference())
                    .log("failed to send order email for order={}", order.getReference(), e);
        }

        cartService.clearCart(username);

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.ORDER_CONFIRMED)
                .addKeyValue(LogFields.Key.USER, username)
                .log("order confirmed user={}", username);
    }

    private AppUser requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void sendEmail(OrderEntity order) {
        var user = order.getUser();
        var sb = new StringBuilder();
        sb.append("Customer: ")
          .append(user.getFirstName()).append(" ").append(user.getLastName())
          .append(" <").append(user.getEmail()).append(">\n\n");

        sb.append("Shipping Address:\n")
          .append("  ").append(order.getStreet()).append("\n")
          .append("  ").append(order.getCity()).append(" ").append(order.getPostalCode()).append("\n")
          .append("  ").append(order.getCountry()).append("\n\n");

        sb.append("Order Lines:\n");
        for (var line : order.getLines()) {
            sb.append(String.format("  %-40s x%-4d %s %.2f%n",
                    line.getProductName(), line.getQuantity(), line.getCurrency(), line.getSubtotal()));
        }

        sb.append(String.format("%nTotal: %s %.2f%n", order.getLines().getFirst().getCurrency(), order.getTotal()));

        var msg = new SimpleMailMessage();
        msg.setTo(orderProperties.adminEmail());
        msg.setSubject("New Order from " + user.getFirstName() + " " + user.getLastName());
        msg.setText(sb.toString());
        mailSender.send(msg);
    }
}
