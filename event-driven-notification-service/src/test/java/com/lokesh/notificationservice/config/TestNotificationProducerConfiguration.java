package com.lokesh.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.notificationservice.kafka.NotificationProducer;
import com.lokesh.notificationservice.model.Notification;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration
@Profile("test")
public class TestNotificationProducerConfiguration {

    private static final AtomicInteger SEND_NOTIFICATION_INVOCATIONS = new AtomicInteger();
    private static final AtomicInteger SEND_BULK_INVOCATIONS = new AtomicInteger();

    public static int sendNotificationInvocations() {
        return SEND_NOTIFICATION_INVOCATIONS.get();
    }

    public static int sendBulkInvocations() {
        return SEND_BULK_INVOCATIONS.get();
    }

    public static void resetCounters() {
        SEND_NOTIFICATION_INVOCATIONS.set(0);
        SEND_BULK_INVOCATIONS.set(0);
    }

    @Bean
    public NotificationProducer notificationProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaTopicProperties topics) {
        return new NotificationProducer(kafkaTemplate, objectMapper, topics) {

            @Override
            public void sendNotification(Notification notification) {
                SEND_NOTIFICATION_INVOCATIONS.incrementAndGet();
                super.sendNotification(notification);
            }

            @Override
            public void sendBulkNotification(List<Notification> notifications) {
                SEND_BULK_INVOCATIONS.incrementAndGet();
                super.sendBulkNotification(notifications);
            }
        };
    }
}
