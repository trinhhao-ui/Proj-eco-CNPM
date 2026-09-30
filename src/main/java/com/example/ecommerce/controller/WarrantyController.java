package com.example.ecommerce.controller;

import com.example.ecommerce.dto.WarrantyInfo;
import com.example.ecommerce.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/warranty")
@RequiredArgsConstructor
public class WarrantyController {

    private final WarrantyService warrantyService;

    @GetMapping
    public String warrantyPage(
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        if (keyword != null && !keyword.isBlank()) {
            keyword = keyword.trim();
            model.addAttribute("keyword", keyword);

            // Check if keyword is pure 15-digit or typical IMEI, or phone number
            if (keyword.length() >= 14 && keyword.matches("\\d+")) {
                WarrantyInfo info = warrantyService.findByImei(keyword);
                if (info != null) {
                    model.addAttribute("warrantyList", List.of(info));
                } else {
                    model.addAttribute("notFound", true);
                }
            } else {
                List<WarrantyInfo> list = warrantyService.findByPhone(keyword);
                if (list != null && !list.isEmpty()) {
                    model.addAttribute("warrantyList", list);
                } else {
                    // Try by IMEI as well just in case
                    WarrantyInfo info = warrantyService.findByImei(keyword);
                    if (info != null) {
                        model.addAttribute("warrantyList", List.of(info));
                    } else {
                        model.addAttribute("notFound", true);
                    }
                }
            }
        }

        return "warranty/lookup";
    }
}
