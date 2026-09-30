package com.example.ecommerce.controller;

import com.example.ecommerce.service.PaymentService;
import com.example.ecommerce.service.VnPayResult;
import com.example.ecommerce.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VnPayService vnPayService;
    private final PaymentService paymentService;

    @GetMapping("/vnpay-return")
    public String vnpayReturn(
            HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        VnPayResult result =
                vnPayService.verifyReturn(request);

        if (result.isValid()) {

            paymentService.handleVnPayResult(result);

            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute(
                        "success",
                        "Thanh toán trực tuyến VNPAY thành công cho đơn hàng #" + result.getOrderId()
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Giao dịch VNPAY không thành công hoặc bạn đã hủy thanh toán (Mã: " + result.getResponseCode() + ")."
                );
            }

            return "redirect:/orders/"
                    + result.getOrderId();
        }

        return "redirect:/payment/result?status=invalid";
    }

    @GetMapping("/result")
    public String paymentResult(
            @RequestParam(required = false) String status,
            org.springframework.ui.Model model) {

        model.addAttribute("status", status);

        return "order/payment-result";
    }
}
