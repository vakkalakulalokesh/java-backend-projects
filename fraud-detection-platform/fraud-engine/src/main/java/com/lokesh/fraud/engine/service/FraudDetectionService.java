package com.lokesh.fraud.engine.service;

import com.lokesh.fraud.common.dto.FraudAlertEvent;
import com.lokesh.fraud.common.dto.RiskAssessment;
import com.lokesh.fraud.common.dto.TransactionEvent;
import com.lokesh.fraud.common.enums.RiskLevel;
import com.lokesh.fraud.engine.engine.RuleEngine;
import com.lokesh.fraud.engine.kafka.FraudResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final RuleEngine ruleEngine;
    private final FraudResultProducer fraudResultProducer;

    public void process(TransactionEvent event) {
        RiskAssessment assessment = ruleEngine.evaluate(event);
        log.info("Assessed {} score {} level {}", event.transactionId(), assessment.riskScore(), assessment.riskLevel());
        fraudResultProducer.publishScored(assessment);
        if (assessment.riskLevel() == RiskLevel.HIGH || assessment.riskLevel() == RiskLevel.CRITICAL) {
            List<String> triggeredNames = assessment.triggeredRules().stream()
                    .filter(r -> r.triggered())
                    .map(com.lokesh.fraud.common.dto.RuleResult::ruleName)
                    .collect(Collectors.toList());
            FraudAlertEvent alert = new FraudAlertEvent(
                    UUID.randomUUID().toString(),
                    event.transactionId(),
                    event.accountId(),
                    assessment.riskScore(),
                    assessment.riskLevel(),
                    triggeredNames,
                    assessment.recommendation(),
                    assessment.assessedAt()
            );
            fraudResultProducer.publishAlert(alert);
        }
    }
}
