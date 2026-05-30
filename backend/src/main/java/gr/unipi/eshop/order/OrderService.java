package gr.unipi.eshop.order;

import gr.unipi.eshop.auth.AppUser;
import gr.unipi.eshop.auth.AppUserRepository;
import gr.unipi.eshop.cart.CartService;
import gr.unipi.eshop.shared.LogFields;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@NullMarked
@Service
public class OrderService {

    private static final String SESSION_KEY = "order";

    private final CartService cartService;
    private final AppUserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String adminEmail;

    public OrderService(CartService cartService,
                        AppUserRepository userRepository,
                        JavaMailSender mailSender,
                        @Value("${app.shop-admin-email}") String adminEmail) {
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.adminEmail = adminEmail;
    }

    public OrderReview submit(HttpSession session, ShippingAddress address) {
        var cart = cartService.view(session);
        if (cart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        var lines = cart.items().stream().map(OrderLine::from).toList();
        var review = new OrderReview(address, lines, cart.total());
        session.setAttribute(SESSION_KEY, review);

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.ORDER_SUBMITTED)
                .addKeyValue("items", lines.size())
                .addKeyValue("total", cart.total())
                .log("order submitted items={} total={}", lines.size(), cart.total());

        return review;
    }

    public void confirm(HttpSession session, String username) {
        if (!(session.getAttribute(SESSION_KEY) instanceof OrderReview review)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending order");
        }

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        sendEmail(review, user);
        cartService.clearCart(session);
        session.removeAttribute(SESSION_KEY);

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.ORDER_CONFIRMED)
                .addKeyValue(LogFields.Key.USER, username)
                .log("order confirmed user={}", username);
    }

    private void sendEmail(OrderReview review, AppUser user) {
        var sb = new StringBuilder();
        sb.append("Customer: ")
          .append(user.getFirstName()).append(" ").append(user.getLastName())
          .append(" <").append(user.getEmail()).append(">\n\n");

        sb.append("Shipping Address:\n")
          .append("  ").append(review.address().street()).append("\n")
          .append("  ").append(review.address().city()).append(" ").append(review.address().postalCode()).append("\n")
          .append("  ").append(review.address().country()).append("\n\n");

        sb.append("Order Lines:\n");
        for (var line : review.lines()) {
            sb.append(String.format("  %-40s x%-4d %s %.2f%n",
                    line.name(), line.quantity(), line.currency(), line.subtotal()));
        }

        sb.append(String.format("%nTotal: %s %.2f%n", review.lines().getFirst().currency(), review.total()));

        var msg = new SimpleMailMessage();
        msg.setTo(adminEmail);
        msg.setSubject("New Order from " + user.getFirstName() + " " + user.getLastName());
        msg.setText(sb.toString());
        mailSender.send(msg);
    }
}
