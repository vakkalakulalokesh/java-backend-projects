package com.lokesh.ecommerce.inventory.config;

import com.lokesh.ecommerce.inventory.kafka.InventoryKafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic ordersInventoryRequest() {
        return TopicBuilder.name(InventoryKafkaTopics.ORDERS_INVENTORY_REQUEST).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersInventoryResponse() {
        return TopicBuilder.name(InventoryKafkaTopics.ORDERS_INVENTORY_RESPONSE).partitions(3).replicas(1).build();
    }
}
