package com.lokesh.fraud.transaction.service;

import com.lokesh.fraud.common.exception.FraudDetectionException;
import com.lokesh.fraud.transaction.dto.TransactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionValidationService {

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR");

    public void validateTransaction(TransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FraudDetectionException("Amount must be positive");
        }
        String currency = request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "";
        if (!ALLOWED_CURRENCIES.contains(currency)) {
            throw new FraudDetectionException("Unsupported currency. Allowed: USD, EUR, GBP, INR");
        }
        String digits = request.getCardNumber().replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) {
            throw new FraudDetectionException("Invalid card number format");
        }
        if (!request.getSourceIp().matches(
                "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$")) {
            throw new FraudDetectionException("Invalid source IP format");
        }
    }

    public String maskCardLastFour(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 4) {
            throw new FraudDetectionException("Invalid card number");
        }
        return "****" + digits.substring(digits.length() - 4);
    }
}
