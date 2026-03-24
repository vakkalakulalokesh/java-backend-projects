package com.lokesh.ecommerce.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic ordersCreated() {
        return TopicBuilder.name(KafkaTopics.ORDERS_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersPaymentRequest() {
        return TopicBuilder.name(KafkaTopics.ORDERS_PAYMENT_REQUEST).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersPaymentResponse() {
        return TopicBuilder.name(KafkaTopics.ORDERS_PAYMENT_RESPONSE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersInventoryRequest() {
        return TopicBuilder.name(KafkaTopics.ORDERS_INVENTORY_REQUEST).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersInventoryResponse() {
        return TopicBuilder.name(KafkaTopics.ORDERS_INVENTORY_RESPONSE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersStatusUpdate() {
        return TopicBuilder.name(KafkaTopics.ORDERS_STATUS_UPDATE).partitions(3).replicas(1).build();
    }
}
