package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/compare")
@RequiredArgsConstructor
public class CompareController {

    private final ProductService productService;

    @GetMapping
    public String compare(
            @RequestParam(required = false) Long id1,
            @RequestParam(required = false) Long id2,
            @RequestParam(required = false) Long id3,
            Model model
    ) {
        Product product1 = (id1 != null) ? productService.findById(id1) : null;
        Product product2 = (id2 != null) ? productService.findById(id2) : null;
        Product product3 = (id3 != null) ? productService.findById(id3) : null;

        List<Product> allProducts = productService.getNewProducts();
        var activePage = productService.getActiveProducts(org.springframework.data.domain.PageRequest.of(0, 100));
        if (activePage != null && !activePage.isEmpty()) {
            allProducts = activePage.getContent();
        }

        model.addAttribute("product1", product1);
        model.addAttribute("product2", product2);
        model.addAttribute("product3", product3);
        model.addAttribute("spec1", product1 != null ? product1.getSpecification() : null);
        model.addAttribute("spec2", product2 != null ? product2.getSpecification() : null);
        model.addAttribute("spec3", product3 != null ? product3.getSpecification() : null);
        model.addAttribute("allProducts", allProducts);

        return "product/compare";
    }
}
