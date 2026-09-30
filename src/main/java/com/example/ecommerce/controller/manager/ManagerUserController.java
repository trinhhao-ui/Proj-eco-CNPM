package com.example.ecommerce.controller.manager;

import com.example.ecommerce.enums.Role;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerUserController {

    private final UserService userService;
    private final OrderService orderService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        model.addAttribute(
                "users",
                userService.filterUsers(keyword, email, fromDate, toDate, pageable)
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("email", email);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("hasFilter", (keyword != null && !keyword.isBlank())
                || (email != null && !email.isBlank())
                || fromDate != null
                || toDate != null);

        return "manager/users/list";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        model.addAttribute(
                "user",
                userService.findById(id)
        );

        model.addAttribute(
                "orders",
                orderService.getUserOrders(
                        id,
                        pageable
                )
        );

        model.addAttribute(
                "totalSpent",
                orderService.getUserTotalSpent(id)
        );

        model.addAttribute(
                "totalOrders",
                orderService.getUserTotalOrders(id)
        );

        return "manager/users/detail";
    }

    @PostMapping("/{id}/role")
    public String updateRole(
            @PathVariable Long id,
            @RequestParam Role role,
            RedirectAttributes redirectAttributes) {

        userService.updateRole(id, role);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã cập nhật quyền người dùng."
        );

        return "redirect:/manager/users/" + id;
    }
}
