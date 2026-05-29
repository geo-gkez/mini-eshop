package gr.unipi.eshop.cart;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class Cart {
    private static final int MAX_ITEMS = 50;

    private final Map<UUID, CartItem> items = new LinkedHashMap<>();

    void add(CartItem item) {
        if (items.size() >= MAX_ITEMS && !items.containsKey(item.productReference())) {
            throw new CartFullException();
        }

        items.merge(
                item.productReference(),
                item,
                (existing, incoming) -> existing.withQuantity(existing.quantity() + incoming.quantity())
        );
    }

    boolean update(UUID reference, int quantity) {
        return items.computeIfPresent(reference, (_, v) -> v.withQuantity(quantity)) != null;
    }

    boolean remove(UUID reference) {
        return items.remove(reference) != null;
    }

    CartResponse toResponse() {
        var total = items.values().stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(List.copyOf(items.values()), total);
    }
}
