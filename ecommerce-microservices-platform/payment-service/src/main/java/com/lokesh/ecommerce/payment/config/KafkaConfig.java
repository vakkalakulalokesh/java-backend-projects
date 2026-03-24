package com.lokesh.ecommerce.payment.config;

import com.lokesh.ecommerce.payment.kafka.PaymentKafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic ordersPaymentRequest() {
        return TopicBuilder.name(PaymentKafkaTopics.ORDERS_PAYMENT_REQUEST).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersPaymentResponse() {
        return TopicBuilder.name(PaymentKafkaTopics.ORDERS_PAYMENT_RESPONSE).partitions(3).replicas(1).build();
    }
}
