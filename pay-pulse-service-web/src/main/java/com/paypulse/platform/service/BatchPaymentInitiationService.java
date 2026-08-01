package com.paypulse.platform.service;

import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.dto.web.response.PaymentBatchCreateResponse;
import com.paypulse.platform.infrastructure.worker.BatchPaymentProcessingWorker;
import com.paypulse.platform.mapper.PaymentBatchCreateResponseMapper;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPaymentInitiationService {

    private final IdempotencyService idempotencyService;
    private final BatchPaymentProcessingWorker batchPaymentProcessingWorker;
    private final PaymentBatchCreateResponseMapper paymentBatchCreateResponseMapper;

    /**
     * Validates a new payment batch submission, performs idempotency checks based on batchId,
     * returns an accepted response immediately, and delegates persistence to a background worker.
     * Behavior:
     * 1. Check for duplicate submission using batchId
     * 2. Validate the request payload
     * 3. Create an accepted response with status PENDING
     * 4. Persist batch and transaction entities asynchronously
     * @param request The payment batch creation request
     * @return PaymentBatchCreateResponse containing batch ID, status, and tracking URL
     */
    public PaymentBatchCreateResponse createBatch(PaymentBatchCreateRequest request) {
        log.debug("Creating payment batch with batchId: {}", request.batchId());

        String generatedBatchId = generateBatchId();
        LocalDateTime acceptedAt = LocalDateTime.now();

        PaymentBatchEntity existingBatch = idempotencyService.reserveSubmission(request.batchId(), acceptedAt);
        if (existingBatch != null) {
            log.warn("Duplicate batch submission detected. ExistingBatchId: {}", existingBatch.getBatchId());
            return paymentBatchCreateResponseMapper.toDuplicateResponse(existingBatch);
        }

        log.debug("Request validation passed for batch: {}", request.batchId());
        batchPaymentProcessingWorker.persistBatchAsync(request, generatedBatchId, acceptedAt);

        return paymentBatchCreateResponseMapper.toAcceptedResponse(generatedBatchId, acceptedAt);
    }

    /**
     * Generates a unique batch ID.
     * Format: BATCH-YYYYMMDD-XXXXXXX (7-char UUID suffix)
     */
    private String generateBatchId() {
        return "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
