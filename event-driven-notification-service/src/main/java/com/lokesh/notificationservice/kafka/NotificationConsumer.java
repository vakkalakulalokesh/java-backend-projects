package com.lokesh.notificationservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.notificationservice.config.KafkaTopicProperties;
import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationStatus;
import com.lokesh.notificationservice.repository.NotificationRepository;
import com.lokesh.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final NotificationProducer notificationProducer;
    private final KafkaTopicProperties kafkaTopicProperties;

    @KafkaListener(topics = "${app.kafka.topics.high-priority}", concurrency = "3",
            containerFactory = "kafkaListenerContainerFactory")
    public void onHighPriority(String payload, Acknowledgment ack) {
        process(payload, ack, kafkaTopicProperties.highPriority());
    }

    @KafkaListener(topics = "${app.kafka.topics.standard}", concurrency = "2",
            containerFactory = "kafkaListenerContainerFactory")
    public void onStandard(String payload, Acknowledgment ack) {
        process(payload, ack, kafkaTopicProperties.standard());
    }

    @KafkaListener(topics = "${app.kafka.topics.bulk}", concurrency = "1",
            containerFactory = "kafkaListenerContainerFactory")
    public void onBulk(String payload, Acknowledgment ack) {
        process(payload, ack, kafkaTopicProperties.bulk());
    }

    private void process(String payload, Acknowledgment ack, String retryTopic) {
        try {
            Notification incoming = objectMapper.readValue(payload, Notification.class);
            UUID id = incoming.getId();
            if (id == null) {
                log.warn("Missing notification id in payload");
                return;
            }
            Notification entity = notificationRepository.findById(id)
                    .orElseGet(() -> notificationRepository.save(incoming));

            boolean success = false;
            try {
                success = notificationDispatcher.dispatch(entity);
            } catch (Exception ex) {
                log.error("Dispatch failed id={}", id, ex);
                Notification failed = notificationRepository.findById(id).orElse(entity);
                failed.setStatus(NotificationStatus.FAILED);
                failed.setErrorMessage(ex.getMessage());
                notificationRepository.save(failed);
                success = false;
            }

            Notification latest = notificationRepository.findById(id).orElse(entity);
            if (!success && latest.getStatus() == NotificationStatus.FAILED) {
                latest.setRetryCount(latest.getRetryCount() + 1);
                if (latest.getRetryCount() >= latest.getMaxRetries()) {
                    notificationRepository.save(latest);
                    notificationProducer.sendToDeadLetterQueue(latest);
                } else {
                    latest.setStatus(NotificationStatus.PENDING);
                    notificationRepository.save(latest);
                    notificationProducer.sendToTopic(retryTopic, latest);
                }
            }
        } catch (Exception e) {
            log.error("Kafka consumer error", e);
        } finally {
            ack.acknowledge();
        }
    }
}
