package com.lokesh.notificationservice.dto;

import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationStatus;

import java.util.Map;

public record NotificationStatsResponse(
        Map<NotificationStatus, Long> byStatus,
        Map<NotificationChannel, Long> byChannel) {
}
