package com.example.ecommerce.controller;

import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public String cart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getUserId();

        model.addAttribute(
                "cart",
                cartService.getOrCreateCart(userId)
        );

        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false, defaultValue = "false") boolean buyNow,
            RedirectAttributes redirectAttributes) {

        boolean success = cartService.addToCart(
                userDetails.getUserId(),
                productId,
                variantId,
                quantity
        );

        if (success) {
            if (buyNow) {
                return "redirect:/checkout";
            }
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã thêm sản phẩm vào giỏ hàng."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể thêm sản phẩm (sản phẩm không khả dụng hoặc vượt quá số lượng tồn kho)."
            );
        }

        return "redirect:/cart";
    }

    @PostMapping("/items/increase/{cartItemId}")
    public String increaseItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId) {
        cartService.increaseItemQuantity(userDetails.getUserId(), cartItemId);
        return "redirect:/cart";
    }

    @PostMapping("/items/decrease/{cartItemId}")
    public String decreaseItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId) {
        cartService.decreaseItemQuantity(userDetails.getUserId(), cartItemId);
        return "redirect:/cart";
    }

    @PostMapping("/items/remove/{cartItemId}")
    public String removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId,
            RedirectAttributes redirectAttributes) {
        cartService.removeCartItem(userDetails.getUserId(), cartItemId);
        redirectAttributes.addFlashAttribute(
                "success",
                "Đã xóa sản phẩm khỏi giỏ hàng."
        );
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            RedirectAttributes redirectAttributes) {

        boolean success = cartService.updateQuantity(
                userDetails.getUserId(),
                productId,
                quantity
        );

        if (success) {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã cập nhật giỏ hàng."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Số lượng cập nhật vượt quá số lượng tồn kho khả dụng."
            );
        }

        return "redirect:/cart";
    }

    @PostMapping("/increase/{productId}")
    public String increase(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {

        cartService.increaseQuantity(
                userDetails.getUserId(),
                productId
        );

        return "redirect:/cart";
    }

    @PostMapping("/decrease/{productId}")
    public String decrease(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {

        cartService.decreaseQuantity(
                userDetails.getUserId(),
                productId
        );

        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    public String remove(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {

        cartService.removeFromCart(
                userDetails.getUserId(),
                productId
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã xóa sản phẩm khỏi giỏ hàng."
        );

        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        cartService.clearCart(userDetails.getUserId());

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã xóa toàn bộ giỏ hàng."
        );

        return "redirect:/cart";
    }
}
