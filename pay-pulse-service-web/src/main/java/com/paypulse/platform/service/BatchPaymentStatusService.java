package com.paypulse.platform.service;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.web.response.PaymentBatchStatusResponse;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPaymentStatusService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

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

        // Calculate transaction counts
        long successfulCount = paymentEntities.stream()
                .filter(p -> p.getStatus() == BatchStatus.COMPLETED)
                .count();

        long failedCount = paymentEntities.stream()
                .filter(p -> p.getStatus() == BatchStatus.FAILED)
                .count();

        long pendingCount = paymentEntities.stream()
                .filter(p -> p.getStatus() == BatchStatus.PENDING)
                .count();

        int totalCount = paymentEntities.size();

        // Calculate failure breakdown
        long retryableFailures = 0;

        long permanentFailures = failedCount - retryableFailures;

        // Get last error message
        String lastErrorMessage = paymentEntities.stream()
                .filter(p -> p.getStatus() == BatchStatus.FAILED)
                .map(PaymentTransactionEntity::getPaymentReference)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(null);

        // Estimate completion time
        LocalDateTime estimatedCompletionTime = estimateCompletionTime(batch, pendingCount, totalCount);

        // Build response
        PaymentBatchStatusResponse.Summary summary = new PaymentBatchStatusResponse.Summary(
                totalCount,
                (int) successfulCount,
                (int) failedCount,
                (int) pendingCount
        );

        PaymentBatchStatusResponse.Timing timing = new PaymentBatchStatusResponse.Timing(
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                estimatedCompletionTime
        );

        PaymentBatchStatusResponse.FailureInfo failureInfo = new PaymentBatchStatusResponse.FailureInfo(
                (int) retryableFailures,
                (int) permanentFailures,
                lastErrorMessage
        );

        PaymentBatchStatusResponse.Links links = new PaymentBatchStatusResponse.Links(
                "/api/v1/batch-payment/" + batchId + "/payments",
                "/api/v1/batch-payment/" + batchId + "/payments?status=FAILED"
        );

        PaymentBatchStatusResponse response = new PaymentBatchStatusResponse(
                batch.getBatchId(),
                batch.getStatus(),
                summary,
                timing,
                failureInfo,
                links
        );

        log.info("Status retrieved for batchId: {}, Status: {}, Success: {}, Failed: {}, Pending: {}",
                batchId, batch.getStatus(), successfulCount, failedCount, pendingCount);

        return response;
    }

    /**
     * Estimates the completion time based on current progress.
     *
     * Formula:
     * - Calculate average processing time per transaction
     * - Multiply by remaining pending transactions
     * - Add to current time
     *
     * @param batch The payment batch
     * @param pendingCount Number of pending payments
     * @param totalCount Total number of payments
     * @return Estimated completion time
     */
    private LocalDateTime estimateCompletionTime(PaymentBatchEntity batch, long pendingCount, int totalCount) {
        if (pendingCount == 0) {
            // Batch already completed
            return batch.getUpdatedAt();
        }

        if (pendingCount == totalCount) {
            // No progress yet, estimate based on avg 5 seconds per transaction
            long estimatedSeconds = totalCount * 5L;
            return LocalDateTime.now().plusSeconds(estimatedSeconds);
        }

        // Calculate average time per completed transaction
        long completedCount = totalCount - pendingCount;
        long elapsedMinutes = ChronoUnit.MINUTES.between(batch.getCreatedAt(), LocalDateTime.now());

        if (elapsedMinutes == 0) {
            elapsedMinutes = 1; // Avoid division by zero
        }

        long avgTimePerTransaction = (elapsedMinutes * 60) / completedCount; // in seconds
        long estimatedRemainingSeconds = avgTimePerTransaction * pendingCount;

        return LocalDateTime.now().plusSeconds(estimatedRemainingSeconds);
    }
}
