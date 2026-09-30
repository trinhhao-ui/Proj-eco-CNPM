package com.example.ecommerce.specification;

import com.example.ecommerce.dto.PhoneFilterCriteria;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductSpecification;
import com.example.ecommerce.entity.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecifications {

    public static Specification<Product> withFilter(PhoneFilterCriteria criteria) {
        return (root, query, cb) -> {
            Specification<Product> spec = Specification.where((r, q, b) -> b.isTrue(r.get("status")));

            if (criteria == null) {
                return spec.toPredicate(root, query, cb);
            }

            criteria.normalizePriceRange();

            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                String pattern = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                spec = spec.and((r, q, b) -> b.or(
                        b.like(b.lower(r.get("name")), pattern),
                        b.like(b.lower(r.get("brand")), pattern),
                        b.like(b.lower(r.get("description")), pattern)
                ));
            }

            if (criteria.getBrand() != null && !criteria.getBrand().isBlank()) {
                spec = spec.and((r, q, b) -> b.equal(b.lower(r.get("brand")), criteria.getBrand().trim().toLowerCase()));
            }

            if (criteria.getCategoryId() != null) {
                spec = spec.and((r, q, b) -> b.equal(r.get("category").get("id"), criteria.getCategoryId()));
            }

            if (criteria.getMinPrice() != null) {
                spec = spec.and((r, q, b) -> b.greaterThanOrEqualTo(r.get("price"), criteria.getMinPrice()));
            }

            if (criteria.getMaxPrice() != null) {
                spec = spec.and((r, q, b) -> b.lessThanOrEqualTo(r.get("price"), criteria.getMaxPrice()));
            }

            if (criteria.getRam() != null && !criteria.getRam().isBlank()) {
                spec = spec.and((r, q, b) -> {
                    Join<Product, ProductSpecification> sJoin = r.join("specification", JoinType.LEFT);
                    return b.like(b.lower(sJoin.get("ram")), "%" + criteria.getRam().trim().toLowerCase() + "%");
                });
            }

            if (criteria.getStorage() != null && !criteria.getStorage().isBlank()) {
                spec = spec.and((r, q, b) -> {
                    q.distinct(true);
                    Join<Product, ProductVariant> vJoin = r.join("variants", JoinType.LEFT);
                    return b.like(b.lower(vJoin.get("storage")), "%" + criteria.getStorage().trim().toLowerCase() + "%");
                });
            }

            if (criteria.getScreenTech() != null && !criteria.getScreenTech().isBlank()) {
                spec = spec.and((r, q, b) -> {
                    Join<Product, ProductSpecification> sJoin = r.join("specification", JoinType.LEFT);
                    return b.like(b.lower(sJoin.get("screenTech")), "%" + criteria.getScreenTech().trim().toLowerCase() + "%");
                });
            }

            if (criteria.getBattery() != null && !criteria.getBattery().isBlank()) {
                spec = spec.and((r, q, b) -> {
                    Join<Product, ProductSpecification> sJoin = r.join("specification", JoinType.LEFT);
                    return b.like(b.lower(sJoin.get("battery")), "%" + criteria.getBattery().trim().toLowerCase() + "%");
                });
            }

            return spec.toPredicate(root, query, cb);
        };
    }
}
