package gr.unipi.eshop.order;

import java.math.BigDecimal;
import java.util.List;

// Response DTO for POST /api/order/submit (the checkout review). Not stored in the session.
public record OrderReview(ShippingAddress address, List<OrderLine> lines, BigDecimal total) {}
