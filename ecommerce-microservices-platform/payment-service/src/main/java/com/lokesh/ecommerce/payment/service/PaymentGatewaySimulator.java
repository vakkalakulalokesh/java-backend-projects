package com.lokesh.ecommerce.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class PaymentGatewaySimulator {

    private static final BigDecimal LIMIT = new BigDecimal("50000");

    public GatewayResult charge(BigDecimal amount) {
        delay();
        if (amount.compareTo(LIMIT) > 0) {
            return GatewayResult.failure("Amount exceeds gateway limit");
        }
        boolean success = ThreadLocalRandom.current().nextDouble() < 0.9;
        if (success) {
            return GatewayResult.success("TXN-" + UUID.randomUUID());
        }
        return GatewayResult.failure("Gateway declined");
    }

    private void delay() {
        int ms = ThreadLocalRandom.current().nextInt(100, 501);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record GatewayResult(boolean ok, String transactionRef, String message) {
        public static GatewayResult success(String ref) {
            return new GatewayResult(true, ref, "OK");
        }

        public static GatewayResult failure(String message) {
            return new GatewayResult(false, null, message);
        }
    }
}
