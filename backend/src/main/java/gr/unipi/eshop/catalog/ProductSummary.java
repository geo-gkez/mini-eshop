package gr.unipi.eshop.catalog;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductSummary(
        UUID reference,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Map<String, Object> attributes
) {
    static ProductSummary from(Product p) {
        return new ProductSummary(
                p.getReference(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCurrency(),
                p.getAttributes()
        );
    }
}
