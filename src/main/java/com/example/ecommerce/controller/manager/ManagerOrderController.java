package com.example.ecommerce.controller.manager;

import com.example.ecommerce.entity.Notification;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.enums.OrderStatus;
import com.example.ecommerce.enums.PaymentMethod;
import com.example.ecommerce.enums.NotificationType;
import com.example.ecommerce.service.NotificationService;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import com.example.ecommerce.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final WebSocketNotificationService webSocketNotificationService;

    @GetMapping
    public String list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        model.addAttribute(
                "orders",
                orderService.filterOrders(
                        status,
                        fromDate,
                        toDate,
                        keyword,
                        pageable
                )
        );

        model.addAttribute(
                "statuses",
                OrderStatus.values()
        );

        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("keyword", keyword);
        model.addAttribute("hasFilter", status != null || fromDate != null || toDate != null || (keyword != null && !keyword.isBlank()));

        return "manager/orders/list";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model) {

        Order order = orderService.findById(id);

        model.addAttribute("order", order);

        model.addAttribute(
                "payment",
                paymentService.findByOrderId(id)
        );

        return "manager/orders/detail";
    }

    @GetMapping("/{id}/invoice")
    public String invoice(
            @PathVariable Long id,
            Model model) {

        Order order = orderService.findById(id);
        if (order == null) {
            return "redirect:/manager/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute(
                "payment",
                paymentService.findByOrderId(id)
        );

        return "order/invoice";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus newStatus,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.findById(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/manager/orders";
        }

        boolean updated = orderService.updateStatus(
                id,
                newStatus
        );

        if (!updated) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể chuyển trạng thái đơn hàng từ " + order.getStatus() + " sang " + newStatus + ". Quy trình chuyển đổi không hợp lệ!"
            );
            return "redirect:/manager/orders/" + id;
        }

        String title;
        String message;

        switch (newStatus) {
            case CONFIRMED -> {
                title = "Đơn hàng đã được xác nhận";
                message = "Đơn hàng #" + order.getId()
                        + " đã được xác nhận.";
            }

            case SHIPPING -> {
                title = "Đơn hàng đang được giao";
                message = "Đơn hàng #" + order.getId()
                        + " đang được giao.";
            }

            case DELIVERED -> {
                title = "Đơn hàng đã giao thành công";
                message = "Đơn hàng #" + order.getId()
                        + " đã được giao thành công.";
            }

            case CANCELLED -> {
                title = "Đơn hàng đã bị hủy";
                message = "Đơn hàng #" + order.getId()
                        + " đã bị hủy.";
            }

            case RETURN_REQUESTED -> {
                title = "Yêu cầu trả hàng";
                message = "Đơn hàng #" + order.getId()
                        + " đang có yêu cầu trả hàng.";
            }

            case REFUNDED -> {
                title = "Đơn hàng đã được hoàn tiền";
                message = "Đơn hàng #" + order.getId()
                        + " đã được hoàn tiền.";
            }

            case RETURN_REJECTED -> {
                title = "Yêu cầu đổi trả bị từ chối";
                message = "Yêu cầu đổi trả/bảo hành cho đơn hàng #" + order.getId()
                        + " đã bị từ chối.";
            }

            default -> {
                title = "Cập nhật đơn hàng";
                message = "Trạng thái đơn hàng #" + order.getId()
                        + " đã được cập nhật.";
            }
        }

        Notification notification =
                notificationService.createNotification(
                        order.getUser().getId(),
                        title,
                        message,
                        NotificationType.ORDER_STATUS
                );

        webSocketNotificationService.sendOrderNotification(
                order.getUser().getId(),
                notification
        );

        com.example.ecommerce.entity.Payment payment = paymentService.findByOrderId(id);

        if (newStatus == OrderStatus.DELIVERED
                && payment != null
                && payment.getMethod() == PaymentMethod.COD) {

            paymentService.markCodAsPaid(id);
        }

        if (newStatus == OrderStatus.REFUNDED) {
            paymentService.refund(id);
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã cập nhật trạng thái đơn hàng."
        );

        return "redirect:/manager/orders/" + id;
    }

    @PostMapping("/{id}/ship")
    public String shipOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String trackingCode,
            @RequestParam java.util.Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.findById(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/manager/orders";
        }

        java.util.Map<Long, String> itemImeis = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("imei_")) {
                try {
                    Long itemId = Long.parseLong(entry.getKey().substring(5));
                    itemImeis.put(itemId, entry.getValue());
                } catch (NumberFormatException ignored) {}
            }
        }

        orderService.assignImeisAndTracking(id, itemImeis, trackingCode);
        boolean updated = orderService.updateStatus(id, OrderStatus.SHIPPING);

        if (!updated) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể chuyển đơn hàng sang 'Đang giao hàng' từ trạng thái hiện tại: " + order.getStatus()
            );
            return "redirect:/manager/orders/" + id;
        }

        if (order.getUser() != null) {
            Notification notification = notificationService.createNotification(
                    order.getUser().getId(),
                    "Đơn hàng đang được giao",
                    "Đơn hàng #" + order.getId() + " đang được giao. Mã vận đơn: "
                            + (trackingCode != null && !trackingCode.isBlank() ? trackingCode : "Đang cập nhật"),
                    NotificationType.ORDER_STATUS
            );
            webSocketNotificationService.sendOrderNotification(order.getUser().getId(), notification);
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã cập nhật mã IMEI và chuyển trạng thái sang Đang giao hàng."
        );

        return "redirect:/manager/orders/" + id;
    }
}
