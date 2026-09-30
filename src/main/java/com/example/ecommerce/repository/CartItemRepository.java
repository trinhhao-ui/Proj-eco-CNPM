package com.example.ecommerce.repository;

import com.example.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    java.util.List<CartItem> findByCartIdAndProductId(
            Long cartId,
            Long productId
    );

    Optional<CartItem> findFirstByCartIdAndProductId(
            Long cartId,
            Long productId
    );

    Optional<CartItem> findByCartIdAndProductIdAndProductVariantId(
            Long cartId,
            Long productId,
            Long productVariantId
    );

    Optional<CartItem> findByCartIdAndProductIdAndProductVariantIsNull(
            Long cartId,
            Long productId
    );

    boolean existsByCartIdAndProductId(
            Long cartId,
            Long productId
    );

    void deleteByCartIdAndProductId(
            Long cartId,
            Long productId
    );

    void deleteByCartId(Long cartId);
}
