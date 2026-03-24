package com.lokesh.fraud.transaction.kafka;

import com.lokesh.fraud.common.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class TransactionEventProducer {

    private static final String TOPIC = "transactions.pending";

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionEventProducer(
            @Qualifier("transactionEventKafkaTemplate") KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTransaction(TransactionEvent event) {
        String correlationId = UUID.randomUUID().toString();
        try (var ignored = MDC.putCloseable("correlationId", correlationId)) {
            kafkaTemplate.send(TOPIC, event.transactionId(), event)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            log.error("[{}] Failed to publish transaction {} to {}", correlationId, event.transactionId(), TOPIC, ex);
                        } else {
                            log.info("[{}] Published transaction {} to {}", correlationId, event.transactionId(), TOPIC);
                        }
                    });
        }
    }
}
