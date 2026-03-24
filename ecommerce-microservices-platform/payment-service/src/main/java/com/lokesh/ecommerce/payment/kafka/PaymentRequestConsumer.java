package com.lokesh.ecommerce.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.payment.dto.PaymentRequest;
import com.lokesh.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @KafkaListener(topics = PaymentKafkaTopics.ORDERS_PAYMENT_REQUEST, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) {
        try {
            Map<String, Object> map = objectMapper.readValue(payload, Map.class);
            PaymentRequest request = PaymentRequest.builder()
                    .orderId((String) map.get("orderId"))
                    .amount(new java.math.BigDecimal(map.get("amount").toString()))
                    .currency(map.get("currency") != null ? map.get("currency").toString() : "USD")
                    .idempotencyKey((String) map.get("idempotencyKey"))
                    .build();
            paymentService.processPayment(request);
        } catch (Exception e) {
            log.error("Payment request handling failed: {}", e.getMessage(), e);
        }
    }
}
