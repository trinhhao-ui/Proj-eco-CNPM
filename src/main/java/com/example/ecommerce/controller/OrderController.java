package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.enums.OrderStatus;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final com.example.ecommerce.service.ReviewService reviewService;
    private final com.example.ecommerce.service.NotificationService notificationService;

    @GetMapping
    public String orders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        Long userId = userDetails.getUserId();

        if (status != null) {
            model.addAttribute(
                    "orders",
                    orderService.getUserOrdersByStatus(
                            userId,
                            status,
                            pageable
                    )
            );
        } else {
            model.addAttribute(
                    "orders",
                    orderService.getUserOrders(
                            userId,
                            pageable
                    )
            );
        }

        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());

        return "order/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            Model model) {

        Long userId = userDetails.getUserId();
        Order order = orderService.findUserOrder(id, userId);

        model.addAttribute("order", order);

        model.addAttribute(
                "payment",
                paymentService.findByOrderId(id)
        );

        java.util.Set<Long> reviewedProductIds = new java.util.HashSet<>();
        if (order != null && order.getItems() != null) {
            for (var item : order.getItems()) {
                if (item.getProduct() != null && reviewService.hasReviewed(userId, item.getProduct().getId(), order.getId())) {
                    reviewedProductIds.add(item.getProduct().getId());
                }
            }
        }
        model.addAttribute("reviewedProductIds", reviewedProductIds);

        return "order/detail";
    }

    @GetMapping("/{id}/invoice")
    public String invoice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Model model) {

        Long userId = userDetails.getUserId();
        Order order = orderService.findUserOrder(id, userId);

        if (order == null) {
            return "redirect:/orders";
        }

        com.example.ecommerce.entity.Payment payment = paymentService.findByOrderId(id);
        boolean isPaid = (payment != null && payment.getStatus() == com.example.ecommerce.enums.PaymentStatus.PAID);
        boolean isDelivered = (order.getStatus() == OrderStatus.DELIVERED);

        if (!isPaid && !isDelivered) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Hóa đơn và phiếu bảo hành chỉ phát hành cho các đơn hàng đã thanh toán thành công hoặc đã nhận máy!"
            );
            return "redirect:/orders/" + id;
        }

        model.addAttribute("order", order);
        model.addAttribute("payment", payment);

        return "order/invoice";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        boolean ok = orderService.cancelOrder(
                id,
                userDetails.getUserId()
        );

        if (ok) {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã hủy đơn hàng thành công."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể hủy đơn hàng (đơn hàng đã được xuất kho vận chuyển hoặc không hợp lệ)."
            );
        }

        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/return")
    public String requestReturn(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(required = false) String returnReason,
            @RequestParam(required = false) String returnImageUrl,
            RedirectAttributes redirectAttributes) {

        boolean ok = orderService.requestReturn(
                id,
                userDetails.getUserId(),
                returnReason,
                returnImageUrl
        );

        if (ok) {
            notificationService.notifyManagers(
                    "Yêu cầu Đổi trả / Bảo hành #" + id,
                    "Khách hàng vừa gửi yêu cầu đổi trả / bảo hành cho đơn hàng #" + id + ". Lý do: " + (returnReason != null && !returnReason.isBlank() ? returnReason : "Không có lý do chi tiết."),
                    com.example.ecommerce.enums.NotificationType.ORDER_STATUS
            );
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã gửi yêu cầu đổi trả / bảo hành thành công."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể gửi yêu cầu đổi trả (đơn hàng chưa được giao thành công hoặc không hợp lệ)."
            );
        }

        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/confirm-received")
    public String confirmReceived(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        boolean ok = orderService.confirmDelivered(id, userDetails.getUserId());
        if (ok) {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Xác nhận nhận hàng thành công! Cảm ơn quý khách đã tin tưởng mua sắm."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể xác nhận nhận hàng (đơn hàng chưa ở trạng thái đang giao hàng)."
            );
        }

        return "redirect:/orders/" + id;
    }

    @GetMapping("/payment-result")
    public String paymentResult(
            @RequestParam(required = false) String status,
            Model model) {

        model.addAttribute("status", status);

        return "order/payment-result";
    }
}
