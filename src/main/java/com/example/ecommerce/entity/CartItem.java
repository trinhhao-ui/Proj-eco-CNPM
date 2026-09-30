package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cart_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cart_items_cart")
    )
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cart_items_product")
    )
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_variant_id",
            foreignKey = @ForeignKey(name = "fk_cart_items_variant")
    )
    private ProductVariant productVariant;

    @Column(nullable = false)
    private Integer quantity;

    public java.math.BigDecimal getEffectivePrice() {
        if (productVariant != null && productVariant.getPrice() != null) {
            return productVariant.getPrice();
        }
        return product != null && product.getPrice() != null ? product.getPrice() : java.math.BigDecimal.ZERO;
    }

    public java.math.BigDecimal getSubtotal() {
        return getEffectivePrice().multiply(java.math.BigDecimal.valueOf(quantity != null ? quantity : 0));
    }
}
