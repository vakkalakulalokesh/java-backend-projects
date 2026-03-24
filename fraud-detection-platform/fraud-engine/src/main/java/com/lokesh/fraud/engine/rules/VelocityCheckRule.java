package com.lokesh.fraud.engine.rules;

import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
@RequiredArgsConstructor
public class VelocityCheckRule implements FraudRule {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${fraud.rules.velocity.window10m-minutes:10}")
    private long window10Minutes;

    @Value("${fraud.rules.velocity.window5m-minutes:5}")
    private long window5Minutes;

    @Value("${fraud.rules.velocity.limit10m:5}")
    private int limit10m;

    @Value("${fraud.rules.velocity.limit5m:3}")
    private int limit5m;

    @Override
    public String getName() {
        return "VelocityCheckRule";
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public RuleResult evaluate(TransactionEvent transaction) {
        String key = "velocity:account:" + transaction.accountId();
        long now = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().add(key, transaction.transactionId(), now);
        stringRedisTemplate.expire(key, Duration.ofMinutes(window10Minutes + 1));

        long cutoff10 = now - TimeUnit.MINUTES.toMillis(window10Minutes);
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff10);

        long cutoff5 = now - TimeUnit.MINUTES.toMillis(window5Minutes);
        Long count10 = stringRedisTemplate.opsForZSet().count(key, cutoff10, now);
        Long count5 = stringRedisTemplate.opsForZSet().count(key, cutoff5, now);
        long c10 = count10 != null ? count10 : 0;
        long c5 = count5 != null ? count5 : 0;

        if (c10 > limit10m) {
            return new RuleResult(getName(), true, 35, "More than " + limit10m + " transactions in " + window10Minutes + "m window");
        }
        if (c5 > limit5m) {
            return new RuleResult(getName(), true, 25, "More than " + limit5m + " transactions in " + window5Minutes + "m window");
        }
        return new RuleResult(getName(), false, 0, "Velocity within limits");
    }
}
