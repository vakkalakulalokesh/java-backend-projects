package com.lokesh.fraud.engine.rules;

import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Order(6)
@RequiredArgsConstructor
public class FrequencyPatternRule implements FraudRule {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${fraud.rules.frequency.window-minutes:5}")
    private long windowMinutes;

    @Override
    public String getName() {
        return "FrequencyPatternRule";
    }

    @Override
    public int getPriority() {
        return 6;
    }

    @Override
    public RuleResult evaluate(TransactionEvent transaction) {
        String key = "freq:amounts:" + transaction.accountId();
        long now = System.currentTimeMillis();
        double score = now;
        String member = transaction.transactionId() + ":" + transaction.amount().toPlainString();
        stringRedisTemplate.opsForZSet().add(key, member, score);
        long cutoff = now - TimeUnit.MINUTES.toMillis(windowMinutes);
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
        stringRedisTemplate.expire(key, java.time.Duration.ofMinutes(windowMinutes + 1));

        Set<String> range = stringRedisTemplate.opsForZSet().rangeByScore(key, cutoff, now);
        if (range == null || range.size() < 3) {
            return new RuleResult(getName(), false, 0, "Insufficient history for card-testing pattern");
        }

        List<BigDecimal> amounts = new ArrayList<>();
        for (String m : range) {
            int idx = m.lastIndexOf(':');
            if (idx > 0 && idx < m.length() - 1) {
                try {
                    amounts.add(new BigDecimal(m.substring(idx + 1)));
                } catch (NumberFormatException ignored) {
                    // skip malformed
                }
            }
        }
        if (amounts.size() < 3) {
            return new RuleResult(getName(), false, 0, "Insufficient parsed amounts");
        }
        amounts.sort(BigDecimal::compareTo);
        boolean increasing = true;
        for (int i = 1; i < amounts.size(); i++) {
            if (amounts.get(i).compareTo(amounts.get(i - 1)) <= 0) {
                increasing = false;
                break;
            }
        }
        if (increasing && amounts.size() >= 3) {
            return new RuleResult(getName(), true, 45, "Monotonic increasing amounts in short window (possible card testing)");
        }
        return new RuleResult(getName(), false, 0, "No rapid increasing-amount sequence");
    }
}
