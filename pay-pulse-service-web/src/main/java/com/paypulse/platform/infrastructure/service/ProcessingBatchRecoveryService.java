package com.paypulse.platform.infrastructure.service;

import com.paypulse.platform.common.properties.BatchProcessingSchedulerProperties;
import com.paypulse.platform.common.dto.BatchStatus;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
import com.paypulse.platform.service.BatchStatusMetricsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingBatchRecoveryService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BatchProcessingSchedulerProperties schedulerProperties;
    private final BatchStatusMetricsCalculator batchStatusMetricsCalculator;

    @Transactional
    public int recoverStuckProcessingBatches() {
        LocalDateTime staleBefore = LocalDateTime.now().minusNanos(schedulerProperties.getStuckBatchTimeoutMillis() * 1_000_000);

        List<String> stuckBatchIds = paymentBatchRepository.findBatchIdsByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                BatchStatus.PROCESSING,
                staleBefore,
                PageRequest.of(0, schedulerProperties.getStuckBatchRecoveryBatchSize())
        );

        int recoveredCount = 0;
        for (String batchId : stuckBatchIds) {
            if (recoverBatch(batchId)) {
                recoveredCount++;
            }
        }

        if (recoveredCount > 0) {
            log.warn("Recovered {} stuck PROCESSING batches", recoveredCount);
        }
        return recoveredCount;
    }

    private boolean recoverBatch(String batchId) {
        PaymentBatchEntity batch = paymentBatchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            return false;
        }

        int currentAttempts = batch.getRecoveryAttemptCount() == null ? 0 : batch.getRecoveryAttemptCount();
        int nextAttempt = currentAttempts + 1;
        LocalDateTime recoveredAt = LocalDateTime.now();
        List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByBatchId(batchId);

        if (nextAttempt >= schedulerProperties.getMaxRecoveryAttempts()) {
            return markBatchAsFailedAfterMaxRecoveryAttempts(batch, transactions, nextAttempt, recoveredAt);
        }

        for (PaymentTransactionEntity transaction : transactions) {
            if (transaction.getStatus() == BatchStatus.PROCESSING) {
                transaction.setStatus(BatchStatus.PENDING);
                transaction.setProcessedAt(null);
                transaction.setFailureReason(null);
                transaction.setRetryable(false);
                transaction.setUpdatedAt(recoveredAt);
            }
        }
        paymentTransactionRepository.saveAll(transactions);

        BatchStatusMetricsCalculator.BatchStatusMetrics metrics = batchStatusMetricsCalculator.calculate(batch, transactions);

        batch.setStatus(metrics.derivedBatchStatus());
        batch.setSuccessfulTransactions(metrics.successfulTransactions());
        batch.setFailedTransactions(metrics.failedTransactions());
        batch.setPendingTransactions(metrics.pendingTransactions());
        batch.setTotalTransactions(metrics.totalTransactions());
        batch.setPaymentsCount(metrics.totalTransactions());
        int completed = metrics.successfulTransactions() + metrics.failedTransactions();
        int progress = metrics.totalTransactions() == 0 ? 0 : (int) ((completed * 100.0) / metrics.totalTransactions());
        batch.setProgressPercentage(progress);
        batch.setRecoveryAttemptCount(nextAttempt);
        batch.setCompletedAt(isTerminalStatus(metrics.derivedBatchStatus()) ? recoveredAt : null);
        batch.setUpdatedAt(recoveredAt);

        paymentBatchRepository.save(batch);
        log.warn("Recovered stuck batchId={} back to PENDING", batchId);
        return true;
    }

    private boolean markBatchAsFailedAfterMaxRecoveryAttempts(
            PaymentBatchEntity batch,
            List<PaymentTransactionEntity> transactions,
            int recoveryAttemptCount,
            LocalDateTime failedAt
    ) {
        for (PaymentTransactionEntity transaction : transactions) {
            if (transaction.getStatus() == BatchStatus.COMPLETED || transaction.getStatus() == BatchStatus.FAILED) {
                continue;
            }

            transaction.setStatus(BatchStatus.FAILED);
            transaction.setFailureReason("Max recovery attempts reached: " + recoveryAttemptCount);
            transaction.setRetryable(false);
            transaction.setProcessedAt(failedAt);
            transaction.setUpdatedAt(failedAt);
        }
        paymentTransactionRepository.saveAll(transactions);

        BatchStatusMetricsCalculator.BatchStatusMetrics metrics = batchStatusMetricsCalculator.calculate(batch, transactions);
        batch.setRecoveryAttemptCount(recoveryAttemptCount);
        batch.setSuccessfulTransactions(metrics.successfulTransactions());
        batch.setFailedTransactions(metrics.failedTransactions());
        batch.setPendingTransactions(metrics.pendingTransactions());
        batch.setTotalTransactions(metrics.totalTransactions());
        batch.setPaymentsCount(metrics.totalTransactions());
        int completed = metrics.successfulTransactions() + metrics.failedTransactions();
        int progress = metrics.totalTransactions() == 0 ? 0 : (int) ((completed * 100.0) / metrics.totalTransactions());
        batch.setProgressPercentage(progress);
        batch.setStatus(metrics.derivedBatchStatus());
        batch.setCompletedAt(failedAt);
        batch.setUpdatedAt(failedAt);

        paymentBatchRepository.save(batch);
        log.error(
                "Batch {} exceeded max recovery attempts ({}). Marked as {}",
                batch.getBatchId(),
                recoveryAttemptCount,
                batch.getStatus()
        );
        return true;
    }

    private boolean isTerminalStatus(BatchStatus status) {
        return status == BatchStatus.COMPLETED
                || status == BatchStatus.FAILED
                || status == BatchStatus.PARTIALLY_COMPLETED;
    }
}

