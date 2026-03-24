package com.lokesh.notificationservice.service;

import com.lokesh.notificationservice.dto.NotificationResponse;
import com.lokesh.notificationservice.exception.NotificationException;
import com.lokesh.notificationservice.handler.NotificationChannelHandler;
import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationStatus;
import com.lokesh.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final List<NotificationChannelHandler> handlers;
    private final NotificationRepository notificationRepository;
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public boolean dispatch(Notification notification) {
        NotificationChannelHandler handler = handlers.stream()
                .filter(h -> h.supports(notification.getChannel()))
                .findFirst()
                .orElseThrow(() -> new NotificationException("No handler for channel: " + notification.getChannel()));

        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);

        boolean success = handler.send(notification).join();
        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            if (notification.getErrorMessage() == null) {
                notification.setErrorMessage("Channel delivery failed");
            }
        }
        notificationRepository.save(notification);
        cacheRecent(notification);
        return success;
    }

    private void cacheRecent(Notification notification) {
        if (redisTemplate == null) {
            return;
        }
        String key = "notification:recent:" + notification.getId();
        redisTemplate.opsForValue().set(key, NotificationResponse.fromEntity(notification), Duration.ofMinutes(5));
        log.debug("Cached notification {}", notification.getId());
    }
}
