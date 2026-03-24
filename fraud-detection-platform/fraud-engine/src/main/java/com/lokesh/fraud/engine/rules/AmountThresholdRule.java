package com.lokesh.fraud.engine.rules;

import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class AmountThresholdRule implements FraudRule {

    @Value("${fraud.rules.amount.threshold-high:10000}")
    private BigDecimal thresholdHigh;

    @Value("${fraud.rules.amount.threshold-medium:5000}")
    private BigDecimal thresholdMedium;

    @Value("${fraud.rules.amount.threshold-low:2000}")
    private BigDecimal thresholdLow;

    @Override
    public String getName() {
        return "AmountThresholdRule";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public RuleResult evaluate(TransactionEvent transaction) {
        BigDecimal amount = transaction.amount();
        if (amount.compareTo(thresholdHigh) > 0) {
            return new RuleResult(getName(), true, 40, "Amount exceeds high threshold " + thresholdHigh);
        }
        if (amount.compareTo(thresholdMedium) > 0) {
            return new RuleResult(getName(), true, 20, "Amount exceeds medium threshold " + thresholdMedium);
        }
        if (amount.compareTo(thresholdLow) > 0) {
            return new RuleResult(getName(), true, 10, "Amount exceeds low threshold " + thresholdLow);
        }
        return new RuleResult(getName(), false, 0, "Amount within normal range");
    }
}
