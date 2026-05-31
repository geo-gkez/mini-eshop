package gr.unipi.eshop.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_lines")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class OrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    static OrderLineEntity from(OrderLine line, OrderEntity order) {
        var entity = new OrderLineEntity();
        entity.order       = order;
        entity.productName = line.name();
        entity.quantity    = line.quantity();
        entity.unitPrice   = line.price();
        entity.currency    = line.currency();
        entity.subtotal    = line.subtotal();
        return entity;
    }
}
