package com.lokesh.notificationservice.dto;

import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotBlank
    private String recipientId;

    @NotNull
    private NotificationChannel channel;

    @Builder.Default
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    private String subject;

    private String content;

    private String templateId;

    @Builder.Default
    private Map<String, String> templateParams = new HashMap<>();

    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    private LocalDateTime scheduledAt;
}
