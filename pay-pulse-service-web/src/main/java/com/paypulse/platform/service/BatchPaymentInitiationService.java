package com.paypulse.platform.service;

import com.paypulse.platform.web.dto.request.PaymentBatchCreateRequest;
import com.paypulse.platform.web.dto.response.PaymentBatchCreateResponse;
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

    public PaymentBatchCreateResponse createBatch(PaymentBatchCreateRequest request) {
        log.debug("Creating payment batch with batchId: {}", request.batchId());

        String generatedBatchId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime acceptedAt = LocalDateTime.now();

        PaymentBatchEntity existingBatch = idempotencyService.reserveBatchPaymentSubmission(request.batchId(), acceptedAt);
        if (existingBatch != null) {
            log.warn("Duplicate batch submission detected. ExistingBatchId: {}", existingBatch.getBatchId());
            return paymentBatchCreateResponseMapper.toDuplicateResponse(existingBatch);
        }

        log.debug("Request validation passed for batch: {}", request.batchId());
        batchPaymentProcessingWorker.persistBatchAsync(request, generatedBatchId, acceptedAt);

        return paymentBatchCreateResponseMapper.toAcceptedResponse(generatedBatchId, acceptedAt);
    }
}
