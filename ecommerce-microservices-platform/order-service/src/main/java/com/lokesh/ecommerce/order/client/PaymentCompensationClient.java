package com.lokesh.ecommerce.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationClient {

    private final RestTemplate paymentRestTemplate;

    public void refund(String paymentId, String reason) {
        try {
            ResponseEntity<Void> response = paymentRestTemplate.postForEntity(
                    "/api/v1/payments/{id}/refund",
                    Map.of("reason", reason == null ? "Saga compensation" : reason),
                    Void.class,
                    paymentId
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Refund non-success status {} for payment {}", response.getStatusCode(), paymentId);
            }
        } catch (RestClientException ex) {
            log.error("Refund failed for payment {}: {}", paymentId, ex.getMessage());
        }
    }
}
