package com.lokesh.fraud.engine.engine;

import com.lokesh.fraud.common.dto.RiskAssessment;
import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;
import com.lokesh.fraud.common.enums.RiskLevel;
import com.lokesh.fraud.engine.rules.FraudRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final List<FraudRule> fraudRules;

    public RiskAssessment evaluate(TransactionEvent transaction) {
        List<FraudRule> ordered = new ArrayList<>(fraudRules);
        ordered.sort(Comparator.comparingInt(FraudRule::getPriority));

        List<RuleResult> results = new ArrayList<>();
        double aggregate = 0;
        for (FraudRule rule : ordered) {
            RuleResult rr = rule.evaluate(transaction);
            results.add(rr);
            if (rr.triggered() && rr.score() > 0) {
                aggregate += rr.score();
            }
        }
        double riskScore = Math.min(100, aggregate);
        RiskLevel level = RiskLevel.fromScore(riskScore);
        String recommendation = buildRecommendation(level, results);
        return new RiskAssessment(
                transaction.transactionId(),
                riskScore,
                level,
                results,
                recommendation,
                LocalDateTime.now()
        );
    }

    private static String buildRecommendation(RiskLevel level, List<RuleResult> results) {
        long triggered = results.stream().filter(RuleResult::triggered).count();
        return switch (level) {
            case LOW -> "Approve. " + triggered + " rules raised signals; overall risk low.";
            case MEDIUM -> "Review recommended. Elevated risk from " + triggered + " signals.";
            case HIGH -> "Hold or step-up authentication. High risk (" + triggered + " signals).";
            case CRITICAL -> "Block or freeze. Critical risk — immediate analyst review.";
        };
    }
}
