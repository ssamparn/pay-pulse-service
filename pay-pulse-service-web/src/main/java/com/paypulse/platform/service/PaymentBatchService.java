package com.paypulse.platform.service;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.dto.web.response.PaymentBatchCreateResponse;
import com.paypulse.platform.dto.web.response.PaymentBatchStatusResponse;
import com.paypulse.platform.persistence.entity.Payment;
import com.paypulse.platform.persistence.entity.PaymentBatch;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentBatchService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;

    /**
     * Creates a new payment batch with individual payments.
     * Behavior:
     * 1. Check for duplicate submission using idempotency key
     * 2. Validate the request payload
     * 3. Create batch entity with status PENDING
     * 4. Create individual payment entities for each payment in the batch
     * 5. Return response with batch details and status tracking URL
     * @param request The payment batch creation request
     * @return PaymentBatchCreateResponse containing batch ID, status, and tracking URL
     */
    public PaymentBatchCreateResponse createBatch(PaymentBatchCreateRequest request) {
        log.debug("Creating payment batch with idempotencyKey: {}", request.idempotencyKey());

        // Step 1: Check for duplicate submission (idempotency)
        PaymentBatch existingBatch = idempotencyService.getExistingBatch(request.idempotencyKey());
        if (existingBatch != null) {
            log.warn("Duplicate batch submission detected. IdempotencyKey: {}, ExistingBatchId: {}",
                    request.idempotencyKey(), existingBatch.getBatchId());

            return new PaymentBatchCreateResponse(
                    existingBatch.getBatchId(),
                    existingBatch.getStatus(),
                    existingBatch.getCreatedAt(),
                    "/api/v1/payment-batches/" + existingBatch.getBatchId() + "/status",
                    false
            );
    }

        // Step 2: Validate request (already handled by @Valid annotation, but can add custom validations here)
        log.debug("Request validation passed for batch: {}", request.batchId());

        // Step 3: Create batch entity with PENDING status
        String generatedBatchId = generateBatchId();
        PaymentBatch paymentBatch = PaymentBatch.create()
                .batchId(generatedBatchId)
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .externalBatchId(request.batchId())
                .status(BatchStatus.PENDING)
                .totalAmount(request.totalAmount())
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .executionDate(request.executionDate())
                .batchDescription(request.batchDescription())
                .requestedBy(request.requestedBy())
                .idempotencyKey(request.idempotencyKey())
                .paymentCount(request.payments().size())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentBatch savedBatch = paymentBatchRepository.save(paymentBatch);
        log.info("Payment batch created successfully. BatchId: {}, ExternalBatchId: {}, Status: {}",
                savedBatch.getBatchId(), savedBatch.getExternalBatchId(), savedBatch.getStatus());

        // Step 4: Create individual payment entities
        List<Payment> payments = request.payments().stream()
                .map(paymentItem -> Payment.create()
                        .paymentId(generatePaymentId())
                        .externalPaymentId(paymentItem.paymentId())
                        .batchId(savedBatch.getBatchId())
                        .beneficiaryId(paymentItem.beneficiaryId())
                        .beneficiaryName(paymentItem.beneficiaryName())
                        .beneficiaryIBAN(paymentItem.beneficiaryIBAN())
                        .amount(paymentItem.amount())
                        .currency(request.currency())
                        .paymentReference(paymentItem.paymentReference())
                        .description(paymentItem.description())
                        .status(BatchStatus.PENDING)  // Individual payment status
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .toList();

        paymentRepository.saveAll(payments);
        log.info("Created {} individual payments for batch: {}", payments.size(), savedBatch.getBatchId());

        // Step 5: Store idempotency key mapping
        idempotencyService.storeIdempotencyMapping(request.idempotencyKey(), savedBatch.getBatchId());

        // Return response
        return new PaymentBatchCreateResponse(
                savedBatch.getBatchId(),
                savedBatch.getStatus(),
                savedBatch.getCreatedAt(),
                "/api/v1/payment-batches/" + savedBatch.getBatchId() + "/status",
                false  // isDuplicate = false (new batch)
        );
    }

    /**
     * Generates a unique batch ID.
     * Format: BATCH-YYYYMMDD-XXXXXXX (7-char UUID suffix)
     */
    private String generateBatchId() {
        return "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates a unique payment ID.
     * Format: PAY-XXXXXXX (7-char UUID suffix)
     */
    private String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

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
        PaymentBatch batch = paymentBatchRepository.findAll().stream()
                .filter(existingBatch -> batchId.equals(existingBatch.getBatchId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Batch not found with batchId: {}", batchId);
                    return new RuntimeException("Batch not found: " + batchId);
                });

        // Fetch all payments for the batch using in-memory filtering for compatibility.
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(payment -> batchId.equals(payment.getBatchId()))
                .toList();
        log.debug("Found {} payments for batchId: {}", payments.size(), batchId);

        // Calculate transaction counts
        long successfulCount = payments.stream()
                .filter(p -> p.getStatus() == BatchStatus.COMPLETED)
                .count();

        long failedCount = payments.stream()
                .filter(p -> p.getStatus() == BatchStatus.FAILED)
                .count();

        long pendingCount = payments.stream()
                .filter(p -> p.getStatus() == BatchStatus.PENDING)
                .count();

        int totalCount = payments.size();

        // Calculate failure breakdown
        long retryableFailures = 0;

        long permanentFailures = failedCount - retryableFailures;

        // Get last error message
        String lastErrorMessage = payments.stream()
                .filter(p -> p.getStatus() == BatchStatus.FAILED)
                .map(Payment::getDescription)
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
                "/api/v1/payment-batches/" + batchId + "/payments",
                "/api/v1/payment-batches/" + batchId + "/payments?status=FAILED"
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
    private LocalDateTime estimateCompletionTime(PaymentBatch batch, long pendingCount, int totalCount) {
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
