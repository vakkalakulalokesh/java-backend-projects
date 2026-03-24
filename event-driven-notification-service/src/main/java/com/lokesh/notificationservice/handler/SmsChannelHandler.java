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
public class SmsChannelHandler implements NotificationChannelHandler {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }

    @Override
    public CompletableFuture<Boolean> send(Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            int delay = ThreadLocalRandom.current().nextInt(20, 81);
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            boolean ok = ThreadLocalRandom.current().nextDouble() < 0.92;
            if (ok) {
                log.info("SMS sent to={} id={}", notification.getRecipientId(), notification.getId());
            } else {
                log.warn("Simulated SMS failure id={}", notification.getId());
            }
            return ok;
        });
    }
}
