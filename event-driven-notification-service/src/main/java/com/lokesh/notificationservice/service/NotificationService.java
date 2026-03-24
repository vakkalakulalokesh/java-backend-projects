package com.lokesh.notificationservice.service;

import com.lokesh.notificationservice.dto.BulkNotificationRequest;
import com.lokesh.notificationservice.dto.NotificationRequest;
import com.lokesh.notificationservice.dto.NotificationResponse;
import com.lokesh.notificationservice.dto.NotificationStatsResponse;
import com.lokesh.notificationservice.kafka.NotificationProducer;
import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationStatus;
import com.lokesh.notificationservice.model.NotificationTemplate;
import com.lokesh.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProducer notificationProducer;
    private final TemplateService templateService;

    @Transactional
    public NotificationResponse send(NotificationRequest request) {
        String subject = request.getSubject();
        String content = request.getContent();
        if (request.getTemplateId() != null && !request.getTemplateId().isBlank()) {
            content = templateService.renderTemplate(request.getTemplateId(), request.getTemplateParams());
            NotificationTemplate template = templateService.getTemplateEntity(request.getTemplateId());
            if (template != null && (subject == null || subject.isBlank())) {
                subject = template.getSubject();
            }
        }
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .channel(request.getChannel())
                .priority(request.getPriority())
                .status(NotificationStatus.PENDING)
                .subject(subject)
                .content(content)
                .metadata(request.getMetadata() != null ? new HashMap<>(request.getMetadata()) : new HashMap<>())
                .templateId(request.getTemplateId())
                .scheduledAt(request.getScheduledAt())
                .build();
        notification = notificationRepository.save(notification);
        notificationProducer.sendNotification(notification);
        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public List<NotificationResponse> sendBulk(BulkNotificationRequest request) {
        List<NotificationResponse> responses = new ArrayList<>();
        List<Notification> batch = new ArrayList<>();
        for (NotificationRequest r : request.getNotifications()) {
            String subject = r.getSubject();
            String content = r.getContent();
            if (r.getTemplateId() != null && !r.getTemplateId().isBlank()) {
                content = templateService.renderTemplate(r.getTemplateId(), r.getTemplateParams());
                NotificationTemplate t = templateService.getTemplateEntity(r.getTemplateId());
                if (t != null && (subject == null || subject.isBlank())) {
                    subject = t.getSubject();
                }
            }
            Notification n = Notification.builder()
                    .recipientId(r.getRecipientId())
                    .channel(r.getChannel())
                    .priority(r.getPriority())
                    .status(NotificationStatus.PENDING)
                    .subject(subject)
                    .content(content)
                    .metadata(r.getMetadata() != null ? new HashMap<>(r.getMetadata()) : new HashMap<>())
                    .templateId(r.getTemplateId())
                    .scheduledAt(r.getScheduledAt())
                    .build();
            n = notificationRepository.save(n);
            batch.add(n);
            responses.add(NotificationResponse.fromEntity(n));
        }
        notificationProducer.sendBulkNotification(batch);
        return responses;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "notifications", key = "#id", unless = "#result == null")
    public NotificationResponse getNotification(UUID id) {
        return notificationRepository.findById(id)
                .map(NotificationResponse::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByRecipient(String recipientId) {
        return notificationRepository.findByRecipientId(recipientId).stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getNotificationStats() {
        Map<NotificationStatus, Long> byStatus = new EnumMap<>(NotificationStatus.class);
        for (NotificationStatus s : NotificationStatus.values()) {
            byStatus.put(s, notificationRepository.countByStatus(s));
        }
        Map<NotificationChannel, Long> byChannel = new EnumMap<>(NotificationChannel.class);
        for (var row : notificationRepository.countByChannelGrouped()) {
            byChannel.put(row.getChannel(), row.getTotal());
        }
        return new NotificationStatsResponse(byStatus, byChannel);
    }
}
