package com.lokesh.fraud.transaction.config;

import com.lokesh.fraud.common.dto.RiskAssessment;
import com.lokesh.fraud.common.dto.TransactionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ProducerFactory<String, TransactionEvent> transactionEventProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, TransactionEvent> transactionEventKafkaTemplate() {
        return new KafkaTemplate<>(transactionEventProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, RiskAssessment> riskAssessmentConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        JsonDeserializer<RiskAssessment> valueDeserializer = new JsonDeserializer<>(RiskAssessment.class, false);
        valueDeserializer.addTrustedPackages("com.lokesh.fraud.common.dto", "com.lokesh.fraud.common.enums");
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RiskAssessment> riskAssessmentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RiskAssessment> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(riskAssessmentConsumerFactory());
        return factory;
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic transactionsPendingTopic() {
        return TopicBuilder.name("transactions.pending").partitions(3).replicas(1).build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic transactionsScoredTopic() {
        return TopicBuilder.name("transactions.scored").partitions(3).replicas(1).build();
    }
}
