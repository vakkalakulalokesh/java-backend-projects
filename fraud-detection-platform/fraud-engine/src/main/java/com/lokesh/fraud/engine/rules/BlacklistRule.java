package com.lokesh.fraud.engine.rules;

import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(4)
@RequiredArgsConstructor
public class BlacklistRule implements FraudRule {

    public static final String MERCHANT_SET = "blacklist:merchants";
    public static final String IP_SET = "blacklist:ips";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String getName() {
        return "BlacklistRule";
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public RuleResult evaluate(TransactionEvent transaction) {
        Boolean badMerchant = stringRedisTemplate.opsForSet().isMember(MERCHANT_SET, transaction.merchantId());
        Boolean badIp = stringRedisTemplate.opsForSet().isMember(IP_SET, transaction.sourceIp());
        if (Boolean.TRUE.equals(badMerchant)) {
            return new RuleResult(getName(), true, 50, "Merchant is blacklisted");
        }
        if (Boolean.TRUE.equals(badIp)) {
            return new RuleResult(getName(), true, 50, "Source IP is blacklisted");
        }
        return new RuleResult(getName(), false, 0, "No blacklist hits");
    }
}
