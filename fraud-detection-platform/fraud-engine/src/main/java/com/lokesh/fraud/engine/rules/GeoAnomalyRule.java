package com.lokesh.fraud.engine.rules;

import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Order(3)
@RequiredArgsConstructor
public class GeoAnomalyRule implements FraudRule {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String getName() {
        return "GeoAnomalyRule";
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public RuleResult evaluate(TransactionEvent transaction) {
        String key = "geo:last:" + transaction.accountId();
        String previous = stringRedisTemplate.opsForValue().get(key);
        String current = extractCountry(transaction.geoLocation());
        stringRedisTemplate.opsForValue().set(key, transaction.geoLocation(), Duration.ofDays(30));

        if (previous == null || previous.isBlank()) {
            return new RuleResult(getName(), false, 0, "No prior geo baseline");
        }
        String prevCountry = extractCountry(previous);
        if (current != null && prevCountry != null && !current.equalsIgnoreCase(prevCountry)) {
            return new RuleResult(getName(), true, 30, "Country changed from " + prevCountry + " to " + current);
        }
        return new RuleResult(getName(), false, 0, "Geo consistent with history");
    }

    private static String extractCountry(String geoLocation) {
        if (geoLocation == null || geoLocation.isBlank()) {
            return null;
        }
        String normalized = geoLocation.trim();
        int dash = normalized.indexOf('-');
        int comma = normalized.indexOf(',');
        int cut = -1;
        if (dash >= 0 && comma >= 0) {
            cut = Math.min(dash, comma);
        } else {
            cut = Math.max(dash, comma);
        }
        if (cut > 0) {
            return normalized.substring(0, cut).trim();
        }
        return normalized;
    }
}
