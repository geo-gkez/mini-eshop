package gr.unipi.eshop.order;

import gr.unipi.eshop.auth.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private UUID reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String street;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String city;

    @Column(name = "postal_code", nullable = false, columnDefinition = "TEXT")
    private String postalCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String country;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineEntity> lines;

    void markEmailSent() {
        this.emailSent = true;
    }

    void markConfirmed() {
        this.status = "CONFIRMED";
    }

    static OrderEntity pending(OrderReview review, AppUser user) {
        var entity = new OrderEntity();
        entity.user = user;
        entity.street = review.address().street();
        entity.city = review.address().city();
        entity.postalCode = review.address().postalCode();
        entity.country = review.address().country();
        entity.total = review.total();
        entity.status = "PENDING";
        entity.lines = new ArrayList<>(review.lines().stream()
                .map(line -> OrderLineEntity.from(line, entity))
                .toList());

        return entity;
    }
}
