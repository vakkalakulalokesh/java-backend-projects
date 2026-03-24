package com.lokesh.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        String highPriority,
        String standard,
        String bulk,
        String deadLetter) {
}
