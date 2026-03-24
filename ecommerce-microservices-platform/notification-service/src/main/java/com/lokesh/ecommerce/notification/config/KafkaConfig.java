package com.lokesh.ecommerce.notification.config;

import com.lokesh.ecommerce.notification.kafka.NotificationTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic ordersStatusUpdate() {
        return TopicBuilder.name(NotificationTopics.ORDERS_STATUS_UPDATE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersPaymentResponse() {
        return TopicBuilder.name(NotificationTopics.ORDERS_PAYMENT_RESPONSE).partitions(3).replicas(1).build();
    }
}
