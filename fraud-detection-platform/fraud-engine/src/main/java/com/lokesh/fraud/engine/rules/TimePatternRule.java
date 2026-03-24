package com.lokesh.fraud.engine.rules;

import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Component
@Order(5)
public class TimePatternRule implements FraudRule {

    private static final BigDecimal WEEKEND_LARGE = new BigDecimal("2500");

    @Override
    public String getName() {
        return "TimePatternRule";
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public RuleResult evaluate(TransactionEvent transaction) {
        LocalDateTime ts = transaction.timestamp() != null ? transaction.timestamp() : LocalDateTime.now();
        int hour = ts.getHour();
        if (hour >= 2 && hour < 5) {
            return new RuleResult(getName(), true, 15, "Transaction during unusual hours (02:00-05:00)");
        }
        DayOfWeek dow = ts.getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        if (weekend && transaction.amount().compareTo(WEEKEND_LARGE) >= 0) {
            return new RuleResult(getName(), true, 10, "Large weekend transaction");
        }
        return new RuleResult(getName(), false, 0, "Time pattern normal");
    }
}
