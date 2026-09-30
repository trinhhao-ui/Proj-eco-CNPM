package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneFilterCriteria {

    private String keyword;
    private String brand;
    private Long categoryId;
    private String priceRange;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String ram;
    private String storage;
    private String screenTech;
    private String battery;
    private String sort;

    public void normalizePriceRange() {
        if (priceRange != null && !priceRange.isBlank()) {
            switch (priceRange) {
                case "under_5m" -> {
                    this.minPrice = BigDecimal.ZERO;
                    this.maxPrice = new BigDecimal("5000000");
                }
                case "5m_10m" -> {
                    this.minPrice = new BigDecimal("5000000");
                    this.maxPrice = new BigDecimal("10000000");
                }
                case "10m_20m" -> {
                    this.minPrice = new BigDecimal("10000000");
                    this.maxPrice = new BigDecimal("20000000");
                }
                case "above_20m" -> {
                    this.minPrice = new BigDecimal("20000000");
                    this.maxPrice = null;
                }
            }
        }
    }

    public boolean hasActiveFilter() {
        return (keyword != null && !keyword.isBlank())
                || (brand != null && !brand.isBlank())
                || categoryId != null
                || (priceRange != null && !priceRange.isBlank())
                || minPrice != null
                || maxPrice != null
                || (ram != null && !ram.isBlank())
                || (storage != null && !storage.isBlank())
                || (screenTech != null && !screenTech.isBlank())
                || (battery != null && !battery.isBlank());
    }
}
