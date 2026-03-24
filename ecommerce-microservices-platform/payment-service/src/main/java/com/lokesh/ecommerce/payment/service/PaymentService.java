package com.lokesh.ecommerce.payment.service;

import com.lokesh.ecommerce.common.enums.PaymentStatus;
import com.lokesh.ecommerce.payment.dto.PaymentRequest;
import com.lokesh.ecommerce.payment.dto.PaymentResponse;
import com.lokesh.ecommerce.payment.dto.RefundRequest;
import com.lokesh.ecommerce.payment.entity.PaymentEntity;
import com.lokesh.ecommerce.payment.kafka.PaymentEventProducer;
import com.lokesh.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewaySimulator gatewaySimulator;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> {
                    replayOutcome(existing);
                    return PaymentResponse.fromEntity(existing);
                })
                .orElseGet(() -> processNewPayment(request));
    }

    private void replayOutcome(PaymentEntity existing) {
        if (existing.getStatus() == PaymentStatus.COMPLETED) {
            paymentEventProducer.publishProcessed(existing);
        } else if (existing.getStatus() == PaymentStatus.FAILED) {
            paymentEventProducer.publishFailed(existing.getOrderId(), existing.getGatewayResponse());
        }
    }

    private PaymentResponse processNewPayment(PaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();
        PaymentEntity entity = PaymentEntity.builder()
                .paymentId(paymentId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(PaymentStatus.PROCESSING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        paymentRepository.save(entity);

        PaymentGatewaySimulator.GatewayResult result = gatewaySimulator.charge(request.getAmount());
        if (result.ok()) {
            entity.setStatus(PaymentStatus.COMPLETED);
            entity.setTransactionRef(result.transactionRef());
            entity.setGatewayResponse(result.message());
            entity.setProcessedAt(Instant.now());
        } else {
            entity.setStatus(PaymentStatus.FAILED);
            entity.setGatewayResponse(result.message());
            entity.setProcessedAt(Instant.now());
        }
        paymentRepository.save(entity);

        if (entity.getStatus() == PaymentStatus.COMPLETED) {
            paymentEventProducer.publishProcessed(entity);
        } else {
            paymentEventProducer.publishFailed(entity.getOrderId(), result.message());
        }
        return PaymentResponse.fromEntity(entity);
    }

    @Transactional
    public PaymentResponse refundPayment(String paymentId, RefundRequest refundRequest) {
        PaymentEntity entity = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        if (entity.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }
        entity.setStatus(PaymentStatus.REFUNDED);
        entity.setGatewayResponse("REFUND: " + (refundRequest.getReason() != null ? refundRequest.getReason() : ""));
        entity.setProcessedAt(Instant.now());
        paymentRepository.save(entity);
        return PaymentResponse.fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .map(PaymentResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentByOrder(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }
}
