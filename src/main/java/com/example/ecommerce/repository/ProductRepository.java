package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.brand <> '' AND p.status = true ORDER BY p.brand ASC")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT s.ram FROM ProductSpecification s WHERE s.ram IS NOT NULL AND s.ram <> '' ORDER BY s.ram ASC")
    List<String> findDistinctRams();

    @Query("SELECT DISTINCT v.storage FROM ProductVariant v WHERE v.storage IS NOT NULL AND v.storage <> '' ORDER BY v.storage ASC")
    List<String> findDistinctStorages();

    Page<Product> findByStatusTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndStatusTrue(
            Long categoryId,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseAndStatusTrue(
            String name,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseAndCategoryIdAndStatusTrue(
            String name,
            Long categoryId,
            Pageable pageable
    );

    Page<Product> findByCategoryId(
            Long categoryId,
            Pageable pageable
    );

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findTop10ByStatusTrueOrderByCreatedAtDesc();

    List<Product> findTop10ByStatusTrueOrderByPriceAsc();

    List<Product> findTop10ByStatusTrueOrderByPriceDesc();

    long countByStatusTrue();
}
