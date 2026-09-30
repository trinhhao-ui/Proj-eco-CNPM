package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyInfo {

    private String imei;
    private String productName;
    private String variantName;
    private String recipientName;
    private String recipientPhone;
    private LocalDateTime purchaseDate;
    private LocalDateTime expiryDate;
    private boolean isExpired;
    private long remainingDays;
}
