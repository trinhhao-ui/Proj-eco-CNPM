package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_specifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_specs_product")
    )
    private Product product;

    @Column(length = 50)
    private String screenSize;

    @Column(length = 100)
    private String screenTech;

    @Column(length = 100)
    private String chipset;

    @Column(length = 50)
    private String ram;

    @Column(length = 50)
    private String battery;

    @Column(length = 50)
    private String chargingWatt;

    @Column(length = 255)
    private String rearCamera;

    @Column(length = 100)
    private String frontCamera;

    @Column(length = 50)
    private String os;
}
