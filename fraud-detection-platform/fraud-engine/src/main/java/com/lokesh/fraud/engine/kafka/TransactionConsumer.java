package com.lokesh.fraud.engine.kafka;

import com.lokesh.fraud.common.dto.TransactionEvent;
import com.lokesh.fraud.engine.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
            topics = "transactions.pending",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3",
            containerFactory = "transactionEventKafkaListenerContainerFactory"
    )
    public void onTransaction(TransactionEvent event) {
        log.debug("Consuming transaction {}", event.transactionId());
        fraudDetectionService.process(event);
    }
}
