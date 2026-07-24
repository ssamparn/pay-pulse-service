package com.paypulse.platform.service;

import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
public class HistoricalDataStalenessCalculator {

    public boolean isStale(List<PaymentBatchEntity> batches, HistoricalDateRange dateRange) {
        PaymentBatchEntity latestBatch = batches.stream()
                .filter(batch -> !batch.getCreatedAt().isAfter(dateRange.to().atTime(23, 59, 59)))
                .max(Comparator.comparing(PaymentBatchEntity::getCreatedAt))
                .orElse(null);

        if (latestBatch == null) {
            return true;
        }

        return ChronoUnit.HOURS.between(latestBatch.getUpdatedAt(), LocalDateTime.now()) > 1;
    }
}