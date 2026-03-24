package com.lokesh.fraud.common.enums;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RiskLevel fromScore(double score) {
        if (score <= 30) {
            return LOW;
        }
        if (score <= 60) {
            return MEDIUM;
        }
        if (score <= 80) {
            return HIGH;
        }
        return CRITICAL;
    }
}
