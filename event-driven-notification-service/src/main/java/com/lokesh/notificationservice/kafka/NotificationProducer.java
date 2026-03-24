package com.lokesh.notificationservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.notificationservice.config.KafkaTopicProperties;
import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties topics;

    public void sendNotification(Notification notification) {
        String topic = switch (notification.getPriority()) {
            case HIGH -> topics.highPriority();
            case MEDIUM, LOW -> topics.standard();
        };
        send(topic, notification);
    }

    public void sendBulkNotification(List<Notification> notifications) {
        for (Notification n : notifications) {
            send(topics.bulk(), n);
        }
    }

    public void sendToDeadLetterQueue(Notification notification) {
        send(topics.deadLetter(), notification);
    }

    public void sendToTopic(String topic, Notification notification) {
        send(topic, notification);
    }

    private void send(String topic, Notification notification) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            String payload = objectMapper.writeValueAsString(notification);
            String key = notification.getId() != null ? notification.getId().toString() : correlationId;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
            record.headers().add(CORRELATION_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(record);
            log.info("Published to topic={} key={} priority={}", topic, key, notification.getPriority());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification", e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
