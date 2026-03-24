package com.lokesh.fraud.engine.controller;

import com.lokesh.fraud.engine.rules.BlacklistRule;
import com.lokesh.fraud.engine.rules.FraudRule;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleConfigController {

    private final List<FraudRule> fraudRules;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping
    public List<RuleInfo> listRules() {
        return fraudRules.stream()
                .sorted(Comparator.comparingInt(FraudRule::getPriority))
                .map(r -> RuleInfo.builder()
                        .name(r.getName())
                        .priority(r.getPriority())
                        .description(describe(r.getName()))
                        .build())
                .collect(Collectors.toList());
    }

    @PostMapping("/blacklist/merchants")
    public Map<String, Object> addMerchant(@RequestBody Map<String, String> body) {
        String id = body.get("merchantId");
        if (id == null || id.isBlank()) {
            return Map.of("ok", false, "error", "merchantId required");
        }
        stringRedisTemplate.opsForSet().add(BlacklistRule.MERCHANT_SET, id.trim());
        return Map.of("ok", true, "merchantId", id.trim());
    }

    @PostMapping("/blacklist/ips")
    public Map<String, Object> addIp(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        if (ip == null || ip.isBlank()) {
            return Map.of("ok", false, "error", "ip required");
        }
        stringRedisTemplate.opsForSet().add(BlacklistRule.IP_SET, ip.trim());
        return Map.of("ok", true, "ip", ip.trim());
    }

    @GetMapping("/velocity/{accountId}")
    public VelocityStats velocity(@PathVariable String accountId) {
        String key = "velocity:account:" + accountId;
        long now = System.currentTimeMillis();
        long cutoff10 = now - java.util.concurrent.TimeUnit.MINUTES.toMillis(10);
        long cutoff5 = now - java.util.concurrent.TimeUnit.MINUTES.toMillis(5);
        Long c10 = stringRedisTemplate.opsForZSet().count(key, cutoff10, now);
        Long c5 = stringRedisTemplate.opsForZSet().count(key, cutoff5, now);
        return VelocityStats.builder()
                .accountId(accountId)
                .countLast10Minutes(c10 != null ? c10 : 0)
                .countLast5Minutes(c5 != null ? c5 : 0)
                .build();
    }

    private static String describe(String name) {
        return switch (name) {
            case "AmountThresholdRule" -> "Tiered amount thresholds with escalating scores";
            case "VelocityCheckRule" -> "Sliding-window transaction counts per account (Redis ZSET)";
            case "GeoAnomalyRule" -> "Country change vs last stored geo per account";
            case "BlacklistRule" -> "Merchant and IP membership in Redis sets";
            case "TimePatternRule" -> "Unusual hours and large weekend spend";
            case "FrequencyPatternRule" -> "Chronological increasing amounts (card testing)";
            default -> "Fraud rule";
        };
    }

    @Data
    @Builder
    public static class RuleInfo {
        private String name;
        private int priority;
        private String description;
    }

    @Data
    @Builder
    public static class VelocityStats {
        private String accountId;
        private long countLast10Minutes;
        private long countLast5Minutes;
    }
}
