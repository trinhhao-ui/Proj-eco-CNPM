package com.example.ecommerce.service;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.enums.OrderStatus;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Page<Review> getProductReviews(
            Long productId,
            Pageable pageable
    ) {
        return reviewRepository
                .findByProductIdOrderByCreatedAtDesc(
                        productId,
                        pageable
                );
    }

    public Page<Review> getUserReviews(
            Long userId,
            Pageable pageable
    ) {
        return reviewRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId,
                        pageable
                );
    }

    public Page<Review> getAllReviews(
            Pageable pageable
    ) {
        return reviewRepository
                .findAllByOrderByCreatedAtDesc(pageable);
    }

    public Review findById(Long id) {
        return reviewRepository.findById(id).orElse(null);
    }

    public boolean hasReviewed(
            Long userId,
            Long productId,
            Long orderId
    ) {
        return reviewRepository
                .existsByUserIdAndProductIdAndOrderId(
                        userId,
                        productId,
                        orderId
                );
    }

    public Long findEligibleOrderIdForReview(Long userId, Long productId) {
        if (userId == null || productId == null) return null;
        var deliveredOrders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId,
                OrderStatus.DELIVERED,
                org.springframework.data.domain.PageRequest.of(0, 50)
        );
        if (deliveredOrders != null && deliveredOrders.hasContent()) {
            for (Order order : deliveredOrders.getContent()) {
                if (order.getItems() != null) {
                    boolean hasItem = order.getItems().stream()
                            .anyMatch(item -> item.getProduct() != null && item.getProduct().getId().equals(productId));
                    if (hasItem && !reviewRepository.existsByUserIdAndProductIdAndOrderId(userId, productId, order.getId())) {
                        return order.getId();
                    }
                }
            }
        }
        return null;
    }

    public boolean hasUserReviewedProduct(Long userId, Long productId) {
        if (userId == null || productId == null) return false;
        return reviewRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public Review createReview(
            Long userId,
            Long productId,
            Long orderId,
            Integer rating,
            String comment
    ) {
        if (userId == null
                || productId == null
                || orderId == null) {
            return null;
        }

        if (rating == null || rating < 1 || rating > 5) {
            return null;
        }

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return null;
        }

        Product product = productRepository
                .findById(productId)
                .orElse(null);

        if (product == null) {
            return null;
        }

        Order order = orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElse(null);

        if (order == null) {
            return null;
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            return null;
        }

        boolean purchased = orderItemRepository
                .existsByOrderIdAndProductId(
                        orderId,
                        productId
                );

        if (!purchased) {
            return null;
        }

        boolean alreadyReviewed =
                reviewRepository
                        .existsByUserIdAndProductIdAndOrderId(
                                userId,
                                productId,
                                orderId
                        );

        if (alreadyReviewed) {
            return null;
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(order)
                .rating(rating)
                .comment(
                        comment != null
                                ? comment.trim()
                                : null
                )
                .build();

        return reviewRepository.save(review);
    }

    @Transactional
    public boolean deleteReview(Long reviewId) {
        Review review = reviewRepository
                .findById(reviewId)
                .orElse(null);

        if (review == null) {
            return false;
        }

        reviewRepository.delete(review);

        return true;
    }
}
