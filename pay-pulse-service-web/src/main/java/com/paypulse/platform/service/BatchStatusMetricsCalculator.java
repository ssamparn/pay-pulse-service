package com.paypulse.platform.service;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class BatchStatusMetricsCalculator {

    public BatchStatusMetrics calculate(PaymentBatchEntity batch, List<PaymentTransactionEntity> paymentTransactions) {
        int totalTransactions = paymentTransactions.size();

        int successfulTransactions = (int) paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.COMPLETED)
                .count();

        int failedTransactions = (int) paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.FAILED)
                .count();

        int pendingTransactions = (int) paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.PENDING)
                .count();

        int retryableFailures = 0;
        int permanentFailures = failedTransactions - retryableFailures;

        String lastErrorMessage = paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.FAILED)
                .map(PaymentTransactionEntity::getPaymentReference)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(null);

        LocalDateTime estimatedCompletionTime = estimateCompletionTime(batch, pendingTransactions, totalTransactions);

        return new BatchStatusMetrics(
                totalTransactions,
                successfulTransactions,
                failedTransactions,
                pendingTransactions,
                retryableFailures,
                permanentFailures,
                lastErrorMessage,
                estimatedCompletionTime
        );
    }

    private LocalDateTime estimateCompletionTime(PaymentBatchEntity batch, int pendingTransactions, int totalTransactions) {
        if (pendingTransactions == 0) {
            return batch.getUpdatedAt();
        }

        if (pendingTransactions == totalTransactions) {
            long estimatedSeconds = totalTransactions * 5L;
            return LocalDateTime.now().plusSeconds(estimatedSeconds);
        }

        int completedTransactions = totalTransactions - pendingTransactions;
        long elapsedMinutes = ChronoUnit.MINUTES.between(batch.getCreatedAt(), LocalDateTime.now());
        if (elapsedMinutes == 0) {
            elapsedMinutes = 1;
        }

        long avgSecondsPerTransaction = (elapsedMinutes * 60) / completedTransactions;
        long estimatedRemainingSeconds = avgSecondsPerTransaction * pendingTransactions;
        return LocalDateTime.now().plusSeconds(estimatedRemainingSeconds);
    }

    public record BatchStatusMetrics(
            Integer totalTransactions,
            Integer successfulTransactions,
            Integer failedTransactions,
            Integer pendingTransactions,
            Integer retryableFailures,
            Integer permanentFailures,
            String lastErrorMessage,
            LocalDateTime estimatedCompletionTime
    ) {
    }
}