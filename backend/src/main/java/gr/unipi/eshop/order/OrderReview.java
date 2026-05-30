package gr.unipi.eshop.order;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record OrderReview(ShippingAddress address, List<OrderLine> lines, BigDecimal total)
        implements Serializable {}
