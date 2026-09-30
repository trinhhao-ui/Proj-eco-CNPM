package com.example.ecommerce.controller.manager;

import com.example.ecommerce.service.CategoryService;
import com.example.ecommerce.service.CloudinaryService;
import com.example.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public String list(
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        model.addAttribute(
                "products",
                productService.getAllProducts(pageable)
        );

        return "manager/products/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {

        model.addAttribute(
                "categories",
                categoryService.getAllCategories()
        );

        return "manager/products/form";
    }

    @PostMapping("/create")
    public String create(
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam(required = false) String brand,
            @RequestParam String description,
            @RequestParam java.math.BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String screenSize,
            @RequestParam(required = false) String screenTech,
            @RequestParam(required = false) String chipset,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String battery,
            @RequestParam(required = false) String chargingWatt,
            @RequestParam(required = false) String rearCamera,
            @RequestParam(required = false) String frontCamera,
            @RequestParam(required = false) String os,
            RedirectAttributes redirectAttributes) {

        var p = productService.createProductWithSpecs(
                name,
                brand,
                description,
                price,
                stock,
                categoryId,
                image,
                screenSize,
                screenTech,
                chipset,
                ram,
                battery,
                chargingWatt,
                rearCamera,
                frontCamera,
                os
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Thêm điện thoại mới và cấu hình thông số thành công."
        );

        if (p != null) {
            return "redirect:/manager/products/" + p.getId() + "/edit";
        }
        return "redirect:/manager/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model) {

        var product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        if (product != null) {
            model.addAttribute("variants", product.getVariants());
            model.addAttribute("spec", product.getSpecification());
        }

        return "manager/products/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam(required = false) String brand,
            @RequestParam String description,
            @RequestParam java.math.BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String screenSize,
            @RequestParam(required = false) String screenTech,
            @RequestParam(required = false) String chipset,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String battery,
            @RequestParam(required = false) String chargingWatt,
            @RequestParam(required = false) String rearCamera,
            @RequestParam(required = false) String frontCamera,
            @RequestParam(required = false) String os,
            RedirectAttributes redirectAttributes) {

        productService.updateProduct(
                id,
                name,
                brand,
                description,
                price,
                stock,
                categoryId,
                image
        );

        productService.saveSpecification(
                id,
                screenSize,
                screenTech,
                chipset,
                ram,
                battery,
                chargingWatt,
                rearCamera,
                frontCamera,
                os
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Cập nhật thông tin và thông số kỹ thuật thành công."
        );

        return "redirect:/manager/products/" + id + "/edit";
    }

    @PostMapping("/{id}/variants/add")
    public String addVariant(
            @PathVariable Long id,
            @RequestParam String storage,
            @RequestParam String color,
            @RequestParam java.math.BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {

        productService.addVariant(id, storage, color, price, stock, sku, image);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã thêm phiên bản máy mới."
        );

        return "redirect:/manager/products/" + id + "/edit";
    }

    @PostMapping("/{id}/variants/{variantId}/update")
    public String updateVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @RequestParam String storage,
            @RequestParam String color,
            @RequestParam java.math.BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam(required = false) String sku,
            RedirectAttributes redirectAttributes) {

        productService.updateVariant(variantId, storage, color, price, stock, sku);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã cập nhật thông tin phiên bản máy."
        );

        return "redirect:/manager/products/" + id + "/edit";
    }

    @PostMapping("/{id}/variants/{variantId}/delete")
    public String deleteVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            RedirectAttributes redirectAttributes) {

        productService.deleteVariant(variantId);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã xóa phiên bản máy."
        );

        return "redirect:/manager/products/" + id + "/edit";
    }

    @PostMapping("/{id}/specs")
    public String saveSpecs(
            @PathVariable Long id,
            @RequestParam(required = false) String screenSize,
            @RequestParam(required = false) String screenTech,
            @RequestParam(required = false) String chipset,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String battery,
            @RequestParam(required = false) String chargingWatt,
            @RequestParam(required = false) String rearCamera,
            @RequestParam(required = false) String frontCamera,
            @RequestParam(required = false) String os,
            RedirectAttributes redirectAttributes) {

        productService.saveSpecification(id, screenSize, screenTech, chipset, ram, battery, chargingWatt, rearCamera, frontCamera, os);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã lưu thông số cấu hình kỹ thuật."
        );

        return "redirect:/manager/products/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        boolean deleted = productService.deleteProduct(id);

        if (deleted) {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã xóa hoàn toàn sản phẩm khỏi hệ thống."
            );
        } else {
            productService.updateStatus(id, false);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Sản phẩm đã có dữ liệu đơn hàng hoặc đánh giá liên quan nên không thể xóa vĩnh viễn. Hệ thống đã tự động chuyển sang trạng thái ẨN KINH DOANH để bảo toàn lịch sử giao dịch!"
            );
        }

        return "redirect:/manager/products";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean status,
            RedirectAttributes redirectAttributes) {

        productService.updateStatus(id, status);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã cập nhật trạng thái sản phẩm."
        );

        return "redirect:/manager/products";
    }
}
