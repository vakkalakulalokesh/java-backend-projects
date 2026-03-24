package com.lokesh.notificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    private String subject;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Builder.Default
    private boolean active = true;

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
