package com.lokesh.notificationservice.handler;

import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationChannel;

import java.util.concurrent.CompletableFuture;

public interface NotificationChannelHandler {

    boolean supports(NotificationChannel channel);

    CompletableFuture<Boolean> send(Notification notification);
}
