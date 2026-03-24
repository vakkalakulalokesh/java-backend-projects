package com.lokesh.notificationservice.dto;

import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationPriority;
import com.lokesh.notificationservice.model.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipientId,
        NotificationChannel channel,
        NotificationPriority priority,
        NotificationStatus status,
        String subject,
        LocalDateTime createdAt,
        LocalDateTime sentAt) {

    public static NotificationResponse fromEntity(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientId(),
                n.getChannel(),
                n.getPriority(),
                n.getStatus(),
                n.getSubject(),
                n.getCreatedAt(),
                n.getSentAt());
    }
}
