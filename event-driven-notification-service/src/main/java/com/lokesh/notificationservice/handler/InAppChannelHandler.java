package com.lokesh.notificationservice.handler;

import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class InAppChannelHandler implements NotificationChannelHandler {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public CompletableFuture<Boolean> send(Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(10, 40));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            String destination = "/topic/notifications/" + notification.getRecipientId();
            messagingTemplate.convertAndSend(destination, Map.of(
                    "notificationId", notification.getId().toString(),
                    "subject", notification.getSubject() != null ? notification.getSubject() : "",
                    "content", notification.getContent() != null ? notification.getContent() : ""));
            log.info("In-app message published destination={} id={}", destination, notification.getId());
            return true;
        });
    }
}
