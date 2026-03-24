package com.lokesh.fraud.alert.config;

import com.lokesh.fraud.common.dto.FraudAlertEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, FraudAlertEvent> fraudAlertConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        JsonDeserializer<FraudAlertEvent> deserializer = new JsonDeserializer<>(FraudAlertEvent.class, false);
        deserializer.addTrustedPackages("com.lokesh.fraud.common.dto", "com.lokesh.fraud.common.enums");
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudAlertEvent> fraudAlertKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FraudAlertEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fraudAlertConsumerFactory());
        return factory;
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic fraudAlertsTopic() {
        return TopicBuilder.name("fraud.alerts").partitions(3).replicas(1).build();
    }
}
