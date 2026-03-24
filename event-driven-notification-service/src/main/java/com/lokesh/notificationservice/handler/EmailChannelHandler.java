package com.lokesh.notificationservice.handler;

import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EmailChannelHandler implements NotificationChannelHandler {

    private static final int MIN_MS = 50;
    private static final int MAX_MS = 200;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public CompletableFuture<Boolean> send(Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            int delay = ThreadLocalRandom.current().nextInt(MIN_MS, MAX_MS + 1);
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            boolean ok = ThreadLocalRandom.current().nextDouble() < 0.95;
            if (ok) {
                log.info("Email dispatched to={} subject={} id={}",
                        notification.getRecipientId(), notification.getSubject(), notification.getId());
            } else {
                log.warn("Simulated email failure id={}", notification.getId());
            }
            return ok;
        });
    }
}
