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

        int pendingOnlyTransactions = (int) paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.PENDING)
                .count();

        int processingTransactions = (int) paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.PROCESSING)
                .count();

        int unfinishedTransactions = pendingOnlyTransactions + processingTransactions;

        BatchStatus derivedBatchStatus = deriveBatchStatus(
                totalTransactions,
                successfulTransactions,
                failedTransactions,
                pendingOnlyTransactions,
                processingTransactions
        );

        int retryableFailures = (int) paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.FAILED)
                .filter(PaymentTransactionEntity::isRetryable)
                .count();
        int permanentFailures = failedTransactions - retryableFailures;

        String lastErrorMessage = paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.FAILED)
                .map(PaymentTransactionEntity::getFailureReason)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(null);

        LocalDateTime estimatedCompletionTime = estimateCompletionTime(batch, unfinishedTransactions, totalTransactions);

        return new BatchStatusMetrics(
                totalTransactions,
                successfulTransactions,
                failedTransactions,
                unfinishedTransactions,
                processingTransactions,
                retryableFailures,
                permanentFailures,
                derivedBatchStatus,
                lastErrorMessage,
                estimatedCompletionTime
        );
    }

    private LocalDateTime estimateCompletionTime(PaymentBatchEntity batch, int unfinishedTransactions, int totalTransactions) {
        if (unfinishedTransactions == 0) {
            return batch.getUpdatedAt();
        }

        if (unfinishedTransactions == totalTransactions) {
            long estimatedSeconds = totalTransactions * 5L;
            return LocalDateTime.now().plusSeconds(estimatedSeconds);
        }

        int completedTransactions = totalTransactions - unfinishedTransactions;
        long elapsedMinutes = ChronoUnit.MINUTES.between(batch.getCreatedAt(), LocalDateTime.now());
        if (elapsedMinutes == 0) {
            elapsedMinutes = 1;
        }

        long avgSecondsPerTransaction = (elapsedMinutes * 60) / completedTransactions;
        long estimatedRemainingSeconds = avgSecondsPerTransaction * unfinishedTransactions;
        return LocalDateTime.now().plusSeconds(estimatedRemainingSeconds);
    }

    private BatchStatus deriveBatchStatus(
            int totalTransactions,
            int successfulTransactions,
            int failedTransactions,
            int pendingOnlyTransactions,
            int processingTransactions
    ) {
        if (totalTransactions == 0) {
            return BatchStatus.PENDING;
        }

        if (pendingOnlyTransactions == totalTransactions && processingTransactions == 0) {
            return BatchStatus.PENDING;
        }

        int unfinishedTransactions = pendingOnlyTransactions + processingTransactions;
        if (unfinishedTransactions > 0) {
            return BatchStatus.PROCESSING;
        }

        if (successfulTransactions == totalTransactions) {
            return BatchStatus.COMPLETED;
        }

        if (failedTransactions == totalTransactions) {
            return BatchStatus.FAILED;
        }

        if (successfulTransactions > 0 && failedTransactions > 0) {
            return BatchStatus.PARTIALLY_COMPLETED;
        }

        return BatchStatus.PROCESSING;
    }

    public record BatchStatusMetrics(
            Integer totalTransactions,
            Integer successfulTransactions,
            Integer failedTransactions,
            Integer pendingTransactions,
            Integer processingTransactions,
            Integer retryableFailures,
            Integer permanentFailures,
            BatchStatus derivedBatchStatus,
            String lastErrorMessage,
            LocalDateTime estimatedCompletionTime
    ) {
    }
}