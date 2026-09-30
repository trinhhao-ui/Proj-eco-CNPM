package com.example.ecommerce.specification;

import com.example.ecommerce.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserSpecifications {

    public static Specification<User> withFilter(String keyword, String email, LocalDate fromDate, LocalDate toDate) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((r, q, b) -> b.or(
                    b.like(b.lower(r.get("name")), pattern),
                    b.like(b.lower(r.get("email")), pattern),
                    b.like(b.lower(r.get("phone")), pattern)
            ));
        }

        if (email != null && !email.isBlank()) {
            String emailPattern = "%" + email.trim().toLowerCase() + "%";
            spec = spec.and((r, q, b) -> b.like(b.lower(r.get("email")), emailPattern));
        }

        if (fromDate != null) {
            LocalDateTime start = fromDate.atStartOfDay();
            spec = spec.and((r, q, b) -> b.greaterThanOrEqualTo(r.get("createdAt"), start));
        }

        if (toDate != null) {
            LocalDateTime end = toDate.atTime(23, 59, 59);
            spec = spec.and((r, q, b) -> b.lessThanOrEqualTo(r.get("createdAt"), end));
        }

        return spec;
    }
}
