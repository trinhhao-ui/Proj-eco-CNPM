package com.example.ecommerce.repository;

import com.example.ecommerce.entity.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {

    Optional<ProductSpecification> findByProductId(Long productId);
}
