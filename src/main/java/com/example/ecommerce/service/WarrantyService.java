package com.example.ecommerce.service;

import com.example.ecommerce.dto.WarrantyInfo;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.enums.OrderStatus;
import com.example.ecommerce.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarrantyService {

    private final OrderItemRepository orderItemRepository;

    public WarrantyInfo findByImei(String imei) {
        if (imei == null || imei.isBlank()) {
            return null;
        }

        OrderItem item = orderItemRepository.findFirstByImei(imei.trim()).orElse(null);
        if (item == null) {
            return null;
        }

        return mapToWarrantyInfo(item);
    }

    public List<WarrantyInfo> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return List.of();
        }

        List<OrderItem> items = orderItemRepository
                .findByOrderRecipientPhoneOrderByOrderCreatedAtDesc(phone.trim());

        List<WarrantyInfo> results = new ArrayList<>();
        for (OrderItem item : items) {
            if (item.getImei() != null && !item.getImei().isBlank()) {
                results.add(mapToWarrantyInfo(item));
            }
        }
        return results;
    }

    private WarrantyInfo mapToWarrantyInfo(OrderItem item) {
        LocalDateTime purchaseDate = item.getOrder() != null ? item.getOrder().getCreatedAt() : LocalDateTime.now();
        // Standard phone warranty: 12 months
        LocalDateTime expiryDate = purchaseDate.plusMonths(12);
        LocalDateTime now = LocalDateTime.now();

        boolean isExpired = now.isAfter(expiryDate);
        long remainingDays = isExpired ? 0 : ChronoUnit.DAYS.between(now, expiryDate);

        return WarrantyInfo.builder()
                .imei(item.getImei())
                .productName(item.getProduct() != null ? item.getProduct().getName() : "")
                .variantName(item.getVariantName())
                .recipientName(item.getOrder() != null ? item.getOrder().getRecipientName() : "")
                .recipientPhone(item.getOrder() != null ? item.getOrder().getRecipientPhone() : "")
                .purchaseDate(purchaseDate)
                .expiryDate(expiryDate)
                .isExpired(isExpired)
                .remainingDays(remainingDays)
                .build();
    }
}
