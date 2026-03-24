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
public class PushChannelHandler implements NotificationChannelHandler {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.PUSH;
    }

    @Override
    public CompletableFuture<Boolean> send(Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(15, 60));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            boolean ok = ThreadLocalRandom.current().nextDouble() < 0.98;
            if (ok) {
                log.info("Push delivered device={} id={}", notification.getRecipientId(), notification.getId());
            } else {
                log.warn("Simulated push failure id={}", notification.getId());
            }
            return ok;
        });
    }
}
