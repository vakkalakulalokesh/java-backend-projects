package com.lokesh.fraud.alert.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DailyCount(LocalDate date, long count) {
}
