package com.example.ecommerce.service;

import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductVariant;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.enums.OrderStatus;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ProductVariantRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;

    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<Order> getUserOrdersByStatus(Long userId, OrderStatus status, Pageable pageable) {
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
    }

    public Order findById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public Order findUserOrder(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId).orElse(null);
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public Page<Order> filterOrders(
            OrderStatus status,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            String keyword,
            Pageable pageable
    ) {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return orderRepository.findAll(
                com.example.ecommerce.specification.OrderSpecifications.withFilter(status, fromDate, toDate, keyword),
                sortedPageable
        );
    }

    public BigDecimal getUserTotalSpent(Long userId) {
        if (userId == null) return BigDecimal.ZERO;
        return orderRepository.sumFinalAmountByUserIdAndStatus(userId, OrderStatus.DELIVERED);
    }

    public long getUserTotalOrders(Long userId) {
        if (userId == null) return 0;
        return orderRepository.countByUserId(userId);
    }

    @Transactional
    public Order createOrder(
            Long userId,
            String recipientName,
            String recipientPhone,
            String shippingAddress,
            String discountCode,
            BigDecimal discountAmount
    ) {
        if (userId == null) return null;
        if (recipientName == null || recipientName.trim().isEmpty()) return null;
        if (recipientPhone == null || recipientPhone.trim().isEmpty()) return null;
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) return null;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) return null;

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product == null || !Boolean.TRUE.equals(product.getStatus())) {
                return null;
            }

            ProductVariant variant = cartItem.getProductVariant();
            int availableStock = variant != null && variant.getStock() != null
                    ? variant.getStock()
                    : (product.getStock() != null ? product.getStock() : 0);

            if (availableStock < cartItem.getQuantity()) {
                return null;
            }

            BigDecimal itemPrice = cartItem.getEffectivePrice();
            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        BigDecimal finalAmount = subtotal.subtract(discountAmount);

        Order order = Order.builder()
                .user(user)
                .recipientName(recipientName.trim())
                .recipientPhone(recipientPhone.trim())
                .shippingAddress(shippingAddress.trim())
                .subtotal(subtotal)
                .discountCode(discountCode != null && !discountCode.isBlank() ? discountCode.trim().toUpperCase() : null)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING)
                .build();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getProductVariant();

            String variantName = null;
            if (variant != null) {
                variantName = (variant.getStorage() != null ? variant.getStorage() : "")
                        + (variant.getColor() != null ? " - " + variant.getColor() : "");
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productVariant(variant)
                    .variantName(variantName)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getEffectivePrice())
                    .build();

            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Deduct stock
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getProductVariant();

            if (variant != null) {
                int currentVariantStock = variant.getStock() != null ? variant.getStock() : 0;
                int newVariantStock = currentVariantStock - cartItem.getQuantity();
                if (newVariantStock < 0) {
                    throw new IllegalStateException("Sản phẩm " + product.getName() + " không đủ số lượng tồn kho.");
                }
                variant.setStock(newVariantStock);
                productVariantRepository.save(variant);

                if (product.getStock() != null) {
                    int newProductStock = product.getStock() - cartItem.getQuantity();
                    product.setStock(Math.max(0, newProductStock));
                    productRepository.save(product);
                }
            } else if (product.getStock() != null) {
                int newProductStock = product.getStock() - cartItem.getQuantity();
                if (newProductStock < 0) {
                    throw new IllegalStateException("Sản phẩm " + product.getName() + " không đủ số lượng tồn kho.");
                }
                product.setStock(newProductStock);
                productRepository.save(product);
            }
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }

    @Transactional
    public Order createOrder(
            Long userId,
            String recipientName,
            String recipientPhone,
            String shippingAddress,
            BigDecimal discountAmount
    ) {
        return createOrder(userId, recipientName, recipientPhone, shippingAddress, null, discountAmount);
    }

    @Transactional
    public boolean cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElse(null);
        if (order == null || (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED)) {
            return false;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        restoreStock(order);

        // If order had a paid payment (e.g. VNPAY), set payment status to REFUNDED
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        return true;
    }

    @Transactional
    public boolean requestReturn(Long orderId, Long userId) {
        return requestReturn(orderId, userId, null, null);
    }

    @Transactional
    public boolean requestReturn(Long orderId, Long userId, String reason, String imageUrl) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.DELIVERED) {
            return false;
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        if (reason != null && !reason.isBlank()) {
            order.setReturnReason(reason.trim());
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            order.setReturnImageUrl(imageUrl.trim());
        }

        orderRepository.save(order);
        return true;
    }

    @Transactional
    public boolean confirmDelivered(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.SHIPPING) {
            return false;
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (payment != null && payment.getStatus() != PaymentStatus.PAID) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
        }

        return true;
    }

    @Transactional
    public boolean updateStatus(Long orderId, OrderStatus newStatus) {
        if (newStatus == null) return false;

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;

        OrderStatus currentStatus = order.getStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            return false;
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REFUNDED) {
            restoreStock(order);
        }

        return true;
    }

    @Transactional
    public boolean assignImeisAndTracking(Long orderId, Map<Long, String> itemImeis, String trackingCode) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;

        if (itemImeis != null) {
            for (OrderItem item : order.getItems()) {
                if (itemImeis.containsKey(item.getId())) {
                    String imei = itemImeis.get(item.getId());
                    if (imei != null && !imei.isBlank()) {
                        item.setImei(imei.trim());
                        orderItemRepository.save(item);
                    }
                }
            }
        }

        if (trackingCode != null && !trackingCode.isBlank()) {
            order.setTrackingCode(trackingCode.trim());
            orderRepository.save(order);
        }

        return true;
    }

    private boolean isValidTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null || newStatus == null) return false;

        return switch (currentStatus) {
            case PENDING ->
                    newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.SHIPPING || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED ->
                    newStatus == OrderStatus.SHIPPING || newStatus == OrderStatus.CANCELLED;
            case SHIPPING ->
                    newStatus == OrderStatus.DELIVERED;
            case DELIVERED ->
                    newStatus == OrderStatus.RETURN_REQUESTED;
            case RETURN_REQUESTED ->
                    newStatus == OrderStatus.REFUNDED || newStatus == OrderStatus.RETURN_REJECTED;
            case CANCELLED, REFUNDED, RETURN_REJECTED ->
                    false;
        };
    }

    private void restoreStock(Order order) {
        if (order.getItems() == null) return;

        for (OrderItem orderItem : order.getItems()) {
            ProductVariant variant = orderItem.getProductVariant();
            if (variant != null) {
                variant.setStock((variant.getStock() != null ? variant.getStock() : 0) + orderItem.getQuantity());
                productVariantRepository.save(variant);
            }

            Product product = orderItem.getProduct();
            if (product != null) {
                product.setStock((product.getStock() != null ? product.getStock() : 0) + orderItem.getQuantity());
                productRepository.save(product);
            }
        }
    }
}
