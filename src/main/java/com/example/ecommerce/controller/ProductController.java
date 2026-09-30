package com.example.ecommerce.controller;

import com.example.ecommerce.service.CategoryService;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.ecommerce.dto.PhoneFilterCriteria;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;

    @GetMapping
    public String listProducts(
            @ModelAttribute PhoneFilterCriteria criteria,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {

        Page<Product> products = productService.filterProducts(criteria, pageable);

        model.addAttribute("products", products);
        model.addAttribute("criteria", criteria);
        model.addAttribute("keyword", criteria.getKeyword());
        model.addAttribute("selectedBrand", criteria.getBrand());
        model.addAttribute("selectedRam", criteria.getRam());
        model.addAttribute("selectedStorage", criteria.getStorage());
        model.addAttribute("selectedPriceRange", criteria.getPriceRange());
        model.addAttribute("selectedSort", criteria.getSort());
        model.addAttribute("hasFilter", criteria.hasActiveFilter());

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("availableBrands", productService.getAvailableBrands());
        model.addAttribute("availableRams", productService.getAvailableRams());
        model.addAttribute("availableStorages", productService.getAvailableStorages());

        return "product/list";
    }

    @GetMapping("/{id}")
    public String productDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        var product = productService.findById(id);
        model.addAttribute("product", product);
        if (product != null) {
            model.addAttribute("variants", product.getVariants());
            model.addAttribute("spec", product.getSpecification());
        }
        model.addAttribute(
                "reviews",
                reviewService.getProductReviews(id, pageable)
        );

        if (userDetails != null) {
            Long eligibleOrderId = reviewService.findEligibleOrderIdForReview(userDetails.getUserId(), id);
            boolean hasReviewed = reviewService.hasUserReviewedProduct(userDetails.getUserId(), id);
            model.addAttribute("canReview", eligibleOrderId != null);
            model.addAttribute("eligibleOrderId", eligibleOrderId);
            model.addAttribute("hasReviewed", hasReviewed);
        } else {
            model.addAttribute("canReview", false);
            model.addAttribute("eligibleOrderId", null);
            model.addAttribute("hasReviewed", false);
        }

        return "product/detail";
    }

    @GetMapping("/search")
    public String searchProducts(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @ModelAttribute PhoneFilterCriteria criteria,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {

        if (keyword != null && !keyword.isBlank()) {
            criteria.setKeyword(keyword.trim());
        }
        return listProducts(criteria, pageable, model);
    }
}
