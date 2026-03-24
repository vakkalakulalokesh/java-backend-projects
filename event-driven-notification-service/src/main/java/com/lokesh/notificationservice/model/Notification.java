package com.lokesh.notificationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationStatus status;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    private String templateId;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 3;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
