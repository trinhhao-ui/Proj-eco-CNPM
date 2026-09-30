package com.example.ecommerce.service;

import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductVariant;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ProductVariantRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        Cart existingCart = cartRepository
                .findByUserId(userId)
                .orElse(null);

        if (existingCart != null) {
            return existingCart;
        }

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            return null;
        }

        Cart cart = Cart.builder()
                .user(user)
                .build();

        return cartRepository.save(cart);
    }

    public Cart getCart(Long userId) {
        return cartRepository
                .findByUserId(userId)
                .orElse(null);
    }

    @Transactional
    public boolean addToCart(
            Long userId,
            Long productId,
            Integer quantity
    ) {
        return addToCart(userId, productId, null, quantity);
    }

    @Transactional
    public boolean addToCart(
            Long userId,
            Long productId,
            Long variantId,
            Integer quantity
    ) {
        if (quantity == null || quantity <= 0) {
            return false;
        }

        Cart cart = getOrCreateCart(userId);
        if (cart == null) {
            return false;
        }

        Product product = productRepository
                .findById(productId)
                .orElse(null);

        if (product == null || !Boolean.TRUE.equals(product.getStatus())) {
            return false;
        }

        ProductVariant variant = null;
        int availableStock = product.getStock() != null ? product.getStock() : 0;

        if (variantId != null) {
            variant = productVariantRepository.findById(variantId).orElse(null);
            if (variant == null || !variant.getProduct().getId().equals(productId)) {
                return false;
            }
            availableStock = variant.getStock() != null ? variant.getStock() : 0;
        }

        if (availableStock < quantity) {
            return false;
        }

        CartItem cartItem;
        if (variant != null) {
            cartItem = cartItemRepository
                    .findByCartIdAndProductIdAndProductVariantId(cart.getId(), productId, variant.getId())
                    .orElse(null);
        } else {
            cartItem = cartItemRepository
                    .findByCartIdAndProductIdAndProductVariantIsNull(cart.getId(), productId)
                    .orElse(null);
        }

        if (cartItem == null) {
            cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .productVariant(variant)
                    .quantity(quantity)
                    .build();
        } else {
            int newQuantity = cartItem.getQuantity() + quantity;
            if (newQuantity > availableStock) {
                return false;
            }
            cartItem.setQuantity(newQuantity);
        }

        cartItemRepository.save(cartItem);
        return true;
    }

    @Transactional
    public boolean increaseQuantity(Long userId, Long productId) {
        Cart cart = getCart(userId);
        if (cart == null) return false;

        CartItem cartItem = cartItemRepository
                .findFirstByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (cartItem == null) return false;
        return increaseItem(cartItem);
    }

    @Transactional
    public boolean increaseItemQuantity(Long userId, Long cartItemId) {
        Cart cart = getCart(userId);
        if (cart == null) return false;

        CartItem cartItem = cartItemRepository.findById(cartItemId).orElse(null);
        if (cartItem == null || !cartItem.getCart().getId().equals(cart.getId())) return false;
        return increaseItem(cartItem);
    }

    private boolean increaseItem(CartItem cartItem) {
        int maxStock = (cartItem.getProductVariant() != null && cartItem.getProductVariant().getStock() != null)
                ? cartItem.getProductVariant().getStock()
                : (cartItem.getProduct().getStock() != null ? cartItem.getProduct().getStock() : 0);

        int newQuantity = cartItem.getQuantity() + 1;
        if (newQuantity > maxStock) {
            return false;
        }
        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
        return true;
    }

    @Transactional
    public boolean decreaseQuantity(Long userId, Long productId) {
        Cart cart = getCart(userId);
        if (cart == null) return false;

        CartItem cartItem = cartItemRepository
                .findFirstByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (cartItem == null) return false;
        return decreaseItem(cartItem);
    }

    @Transactional
    public boolean decreaseItemQuantity(Long userId, Long cartItemId) {
        Cart cart = getCart(userId);
        if (cart == null) return false;

        CartItem cartItem = cartItemRepository.findById(cartItemId).orElse(null);
        if (cartItem == null || !cartItem.getCart().getId().equals(cart.getId())) return false;
        return decreaseItem(cartItem);
    }

    private boolean decreaseItem(CartItem cartItem) {
        if (cartItem.getQuantity() <= 1) {
            cartItemRepository.delete(cartItem);
            return true;
        }
        cartItem.setQuantity(cartItem.getQuantity() - 1);
        cartItemRepository.save(cartItem);
        return true;
    }

    @Transactional
    public boolean removeFromCart(Long userId, Long productId) {
        Cart cart = getCart(userId);
        if (cart == null) return false;

        cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
        return true;
    }

    @Transactional
    public boolean removeCartItem(Long userId, Long cartItemId) {
        Cart cart = getCart(userId);
        if (cart == null) return false;

        CartItem cartItem = cartItemRepository.findById(cartItemId).orElse(null);
        if (cartItem == null || !cartItem.getCart().getId().equals(cart.getId())) return false;
        cartItemRepository.delete(cartItem);
        return true;
    }

    @Transactional
    public boolean updateQuantity(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) return false;
        Cart cart = getCart(userId);
        if (cart == null) return false;

        CartItem cartItem = cartItemRepository
                .findFirstByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (cartItem == null) return false;

        int maxStock = (cartItem.getProductVariant() != null && cartItem.getProductVariant().getStock() != null)
                ? cartItem.getProductVariant().getStock()
                : (cartItem.getProduct().getStock() != null ? cartItem.getProduct().getStock() : 0);

        if (quantity > maxStock) return false;
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        return true;
    }

    @Transactional
    public boolean clearCart(Long userId) {
        Cart cart = getCart(userId);
        if (cart == null) return false;
        cartItemRepository.deleteByCartId(cart.getId());
        return true;
    }
}
