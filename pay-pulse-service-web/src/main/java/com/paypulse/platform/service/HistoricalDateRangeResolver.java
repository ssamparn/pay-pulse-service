package com.paypulse.platform.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HistoricalDateRangeResolver {

    public HistoricalDateRange resolve(String period, LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();

        if (period != null) {
            return switch (period.toUpperCase()) {
                case "LAST_3_MONTHS" -> new HistoricalDateRange(today.minusMonths(3), today);
                case "LAST_6_MONTHS" -> new HistoricalDateRange(today.minusMonths(6), today);
                default -> throw new IllegalArgumentException("Invalid period: " + period);
            };
        }

        if (fromDate != null && toDate != null) {
            return new HistoricalDateRange(fromDate, toDate);
        }

        return new HistoricalDateRange(today.minusMonths(3), today);
    }
}