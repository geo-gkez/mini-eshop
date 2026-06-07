package gr.unipi.eshop.order;

import gr.unipi.eshop.auth.AppUser;
import gr.unipi.eshop.shared.LogFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the admin notification e-mail for a confirmed order.
 *
 * <p>Runs AFTER_COMMIT of the checkout transaction so SMTP latency never holds the DB connection
 * open. A failure here leaves the order CONFIRMED with {@code email_sent = false} — ops can find and
 * resend unsent orders via {@code WHERE email_sent = false}. The send must not roll back the order.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
class OrderEmailSender {

    private final JavaMailSender mailSender;
    private final OrderRepository orderRepository;
    private final OrderProperties orderProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        var order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            return; // order vanished between commit and send — nothing to notify about
        }

        try {
            mailSender.send(buildMessage(order));
            order.markEmailSent();
        } catch (Exception e) {
            log.atError()
                    .addKeyValue(LogFields.Key.EVENT, LogFields.Event.ORDER_EMAIL_FAILED)
                    .addKeyValue("orderReference", order.getReference())
                    .log("failed to send order email for order={}", order.getReference(), e);
        }
    }

    private SimpleMailMessage buildMessage(OrderEntity order) {
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
        msg.setFrom(orderProperties.fromEmail());
        msg.setTo(orderProperties.adminEmail());
        msg.setSubject("New Order from " + sanitizeHeader(displayName(user)));
        msg.setText(sb.toString());

        return msg;
    }

    private static String displayName(AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static String sanitizeHeader(String value) {
        return value.replaceAll("[\\r\\n]", " ");
    }
}
