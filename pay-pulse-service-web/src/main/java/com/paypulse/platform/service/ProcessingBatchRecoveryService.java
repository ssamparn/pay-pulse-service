package com.paypulse.platform.service;

import com.paypulse.platform.config.BatchProcessingSchedulerProperties;
import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
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

        long successful = transactions.stream().filter(tx -> tx.getStatus() == BatchStatus.COMPLETED).count();
        long failed = transactions.stream().filter(tx -> tx.getStatus() == BatchStatus.FAILED).count();
        long pending = transactions.stream().filter(tx -> tx.getStatus() == BatchStatus.PENDING).count();

        batch.setStatus(BatchStatus.PENDING);
        batch.setSuccessfulTransactions((int) successful);
        batch.setFailedTransactions((int) failed);
        batch.setPendingTransactions((int) pending);
        batch.setTotalTransactions(transactions.size());
        batch.setPaymentsCount(transactions.size());
        batch.setProgressPercentage(transactions.isEmpty() ? 0 : (int) (((successful + failed) * 100.0) / transactions.size()));
        batch.setRecoveryAttemptCount(nextAttempt);
        batch.setCompletedAt(null);
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

        long successful = transactions.stream().filter(tx -> tx.getStatus() == BatchStatus.COMPLETED).count();
        long failed = transactions.stream().filter(tx -> tx.getStatus() == BatchStatus.FAILED).count();

        batch.setRecoveryAttemptCount(recoveryAttemptCount);
        batch.setSuccessfulTransactions((int) successful);
        batch.setFailedTransactions((int) failed);
        batch.setPendingTransactions(0);
        batch.setTotalTransactions(transactions.size());
        batch.setPaymentsCount(transactions.size());
        batch.setProgressPercentage(transactions.isEmpty() ? 0 : (int) (((successful + failed) * 100.0) / transactions.size()));
        batch.setStatus(successful > 0 ? BatchStatus.PARTIALLY_COMPLETED : BatchStatus.FAILED);
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
}

