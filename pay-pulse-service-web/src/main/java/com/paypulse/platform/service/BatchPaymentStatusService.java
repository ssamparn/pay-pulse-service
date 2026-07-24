package com.paypulse.platform.service;

import com.paypulse.platform.dto.web.response.PaymentBatchStatusResponse;
import com.paypulse.platform.mapper.PaymentBatchStatusResponseMapper;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPaymentStatusService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentBatchStatusResponseMapper paymentBatchStatusResponseMapper;
    private final BatchStatusMetricsCalculator batchStatusMetricsCalculator;

    /**
     * Retrieves the current status of a payment batch.
     *
     * Calculates:
     * - Transaction counts (successful, failed, pending)
     * - Failure breakdown (retryable vs permanent)
     * - Estimated completion time
     * - Links to detailed payment information
     *
     * @param batchId The batch ID to query
     * @return PaymentBatchStatusResponse with current batch status and metrics
     * @throws RuntimeException if batch not found
     */
    @Transactional(readOnly = true)
    public PaymentBatchStatusResponse getBatchStatus(String batchId) {
        log.debug("Fetching status for batchId: {}", batchId);

        // Fetch batch from repository; use filtering because repository ID type may differ from API batchId type.
        PaymentBatchEntity batch = paymentBatchRepository.findAll().stream()
                .filter(existingBatch -> batchId.equals(existingBatch.getBatchId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Batch not found with batchId: {}", batchId);
                    return new RuntimeException("Batch not found: " + batchId);
                });

        // Fetch all payments for the batch using in-memory filtering for compatibility.
        List<PaymentTransactionEntity> paymentEntities = paymentTransactionRepository.findAll().stream()
                .filter(paymentEntity -> batchId.equals(paymentEntity.getBatchId()))
                .toList();
        log.debug("Found {} payments for batchId: {}", paymentEntities.size(), batchId);

        BatchStatusMetricsCalculator.BatchStatusMetrics metrics =
                batchStatusMetricsCalculator.calculate(batch, paymentEntities);

        PaymentBatchStatusResponse response = paymentBatchStatusResponseMapper.toResponse(
                batch,
                metrics.totalTransactions(),
                metrics.successfulTransactions(),
                metrics.failedTransactions(),
                metrics.pendingTransactions(),
                metrics.retryableFailures(),
                metrics.permanentFailures(),
                metrics.lastErrorMessage(),
                metrics.estimatedCompletionTime()
        );

        log.info("Status retrieved for batchId: {}, Status: {}, Success: {}, Failed: {}, Pending: {}",
                batchId,
                batch.getStatus(),
                metrics.successfulTransactions(),
                metrics.failedTransactions(),
                metrics.pendingTransactions());

        return response;
    }
}
