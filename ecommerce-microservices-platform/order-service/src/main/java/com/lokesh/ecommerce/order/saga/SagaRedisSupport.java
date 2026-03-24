package com.lokesh.ecommerce.order.saga;

import com.lokesh.ecommerce.common.enums.SagaStatus;
import com.lokesh.ecommerce.order.entity.SagaState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SagaRedisSupport {

    private static final String KEY_PREFIX = "saga:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> orderRedisTemplate;

    public void put(SagaState state) {
        String key = KEY_PREFIX + state.getSagaId();
        Map<String, Object> map = new HashMap<>();
        map.put("sagaId", state.getSagaId());
        map.put("orderId", state.getOrderId());
        map.put("currentStep", state.getCurrentStep().name());
        map.put("failed", state.isFailed());
        orderRedisTemplate.opsForValue().set(key, map, TTL);
        orderRedisTemplate.opsForValue().set(KEY_PREFIX + "order:" + state.getOrderId(), state.getSagaId(), TTL);
    }

    public Optional<SagaStatus> quickStatus(String sagaId) {
        Object raw = orderRedisTemplate.opsForValue().get(KEY_PREFIX + sagaId);
        if (raw instanceof Map<?, ?> m && m.get("currentStep") instanceof String s) {
            return Optional.of(SagaStatus.valueOf(s));
        }
        return Optional.empty();
    }

    public Optional<String> findSagaIdByOrder(String orderId) {
        Object v = orderRedisTemplate.opsForValue().get(KEY_PREFIX + "order:" + orderId);
        if (v instanceof String s) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    public void evict(String sagaId, String orderId) {
        orderRedisTemplate.delete(KEY_PREFIX + sagaId);
        orderRedisTemplate.delete(KEY_PREFIX + "order:" + orderId);
    }
}
