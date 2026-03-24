package com.lokesh.notificationservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationStatus;
import com.lokesh.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "${app.kafka.topics.dead-letter}", concurrency = "1",
            containerFactory = "kafkaListenerContainerFactory")
    public void onDeadLetter(String payload, Acknowledgment ack) {
        try {
            Notification incoming = objectMapper.readValue(payload, Notification.class);
            if (incoming.getId() == null) {
                log.warn("DLQ payload without id");
                return;
            }
            notificationRepository.findById(incoming.getId()).ifPresentOrElse(entity -> {
                entity.setStatus(NotificationStatus.DLQ);
                if (entity.getErrorMessage() == null || entity.getErrorMessage().isBlank()) {
                    entity.setErrorMessage("Exceeded max retries — routed to dead-letter topic");
                }
                notificationRepository.save(entity);
                log.error("DLQ_ALERT id={} recipient={} channel={} retries={}",
                        entity.getId(), entity.getRecipientId(), entity.getChannel(), entity.getRetryCount());
            }, () -> log.warn("DLQ message for unknown id={}", incoming.getId()));
        } catch (Exception e) {
            log.error("DLQ consumer failed to process payload", e);
        } finally {
            ack.acknowledge();
        }
    }
}
