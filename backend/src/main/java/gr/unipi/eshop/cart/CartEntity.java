package gr.unipi.eshop.cart;

import gr.unipi.eshop.auth.AppUser;
import gr.unipi.eshop.catalog.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CartEntity {

    private static final int MAX_ITEMS = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CartItemEntity> items = new ArrayList<>();

    CartEntity(AppUser user) {
        this.user = user;
    }

    void add(Product product, int quantity) {
        var existing = findByProduct(product.getReference());
        if (existing.isPresent()) {
            existing.get().addQuantity(quantity);
            return;
        }
        if (items.size() >= MAX_ITEMS) {
            throw new CartFullException();
        }
        items.add(new CartItemEntity(this, product, quantity));
    }

    boolean update(UUID productReference, int quantity) {
        var item = findByProduct(productReference);
        item.ifPresent(i -> i.setQuantity(quantity));
        return item.isPresent();
    }

    boolean remove(UUID productReference) {
        return items.removeIf(i -> i.getProduct().getReference().equals(productReference));
    }

    CartResponse toResponse() {
        var lines = items.stream()
                .map(i -> CartItem.of(i.getProduct(), i.getQuantity()))
                .toList();
        var total = lines.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(lines, total);
    }

    private java.util.Optional<CartItemEntity> findByProduct(UUID productReference) {
        return items.stream()
                .filter(i -> i.getProduct().getReference().equals(productReference))
                .findFirst();
    }
}
