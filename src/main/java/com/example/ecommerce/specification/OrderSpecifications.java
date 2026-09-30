package com.example.ecommerce.specification;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderSpecifications {

    public static Specification<Order> withFilter(OrderStatus status, LocalDate fromDate, LocalDate toDate, String keyword) {
        Specification<Order> spec = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            spec = spec.and((r, q, b) -> b.equal(r.get("status"), status));
        }

        if (fromDate != null) {
            LocalDateTime start = fromDate.atStartOfDay();
            spec = spec.and((r, q, b) -> b.greaterThanOrEqualTo(r.get("createdAt"), start));
        }

        if (toDate != null) {
            LocalDateTime end = toDate.atTime(23, 59, 59);
            spec = spec.and((r, q, b) -> b.lessThanOrEqualTo(r.get("createdAt"), end));
        }

        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            String pattern = "%" + trimmed.toLowerCase() + "%";

            spec = spec.and((r, q, b) -> {
                var predicates = b.or(
                        b.like(b.lower(r.get("recipientName")), pattern),
                        b.like(b.lower(r.get("recipientPhone")), pattern),
                        b.like(b.lower(r.get("user").get("email")), pattern)
                );

                try {
                    Long orderId = Long.parseLong(trimmed.replace("#", ""));
                    return b.or(predicates, b.equal(r.get("id"), orderId));
                } catch (NumberFormatException e) {
                    return predicates;
                }
            });
        }

        return spec;
    }
}
