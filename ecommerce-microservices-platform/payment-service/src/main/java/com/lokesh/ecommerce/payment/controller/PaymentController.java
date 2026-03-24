package com.lokesh.ecommerce.payment.controller;

import com.lokesh.ecommerce.payment.dto.PaymentResponse;
import com.lokesh.ecommerce.payment.dto.RefundRequest;
import com.lokesh.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by id")
    public PaymentResponse get(@PathVariable String id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Payments for order")
    public List<PaymentResponse> byOrder(@PathVariable String orderId) {
        return paymentService.getPaymentByOrder(orderId);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund payment")
    public PaymentResponse refund(@PathVariable String id, @Valid @RequestBody RefundRequest request) {
        request.setPaymentId(id);
        return paymentService.refundPayment(id, request);
    }
}
