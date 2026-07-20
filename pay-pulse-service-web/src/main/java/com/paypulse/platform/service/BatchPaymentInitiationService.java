package com.paypulse.platform.service;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.dto.web.response.PaymentBatchCreateResponse;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
import com.paypulse.platform.persistence.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPaymentInitiationService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
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
        PaymentBatchEntity existingBatch = idempotencyService.getExistingBatch(request.idempotencyKey());
        if (existingBatch != null) {
            log.warn("Duplicate batch submission detected. IdempotencyKey: {}, ExistingBatchId: {}",
                    request.idempotencyKey(), existingBatch.getBatchId());

            return new PaymentBatchCreateResponse(
                    existingBatch.getBatchId(),
                    existingBatch.getStatus(),
                    existingBatch.getCreatedAt(),
                    "/api/v1/batch-payment/" + existingBatch.getBatchId() + "/status",
                    false
            );
        }

        // Step 2: Validate request (already handled by @Valid annotation, but can add custom validations here)
        log.debug("Request validation passed for batch: {}", request.batchId());

        // Step 3: Create batch entity with PENDING status
        String generatedBatchId = generateBatchId();
        int totalTransactions = request.payments().size();
        PaymentBatchEntity paymentBatchEntity = PaymentBatchEntity.create()
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
                .paymentsCount(totalTransactions)
                .totalTransactions(totalTransactions)
                .successfulTransactions(0)
                .failedTransactions(0)
                .pendingTransactions(totalTransactions)
                .progressPercentage(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentBatchEntity savedBatch = paymentBatchRepository.save(paymentBatchEntity);
        log.info("Payment batch created successfully. BatchId: {}, ExternalBatchId: {}, Status: {}",
                savedBatch.getBatchId(), savedBatch.getExternalBatchId(), savedBatch.getStatus());

        // Step 4: Create individual payment entities
        List<PaymentTransactionEntity> paymentEntities = request.payments().stream()
                .map(paymentItem -> PaymentTransactionEntity.create()
                        .paymentId(generatePaymentId())
                        .externalPaymentId(paymentItem.paymentId())
                        .batchId(savedBatch.getBatchId())
                        .beneficiaryId(paymentItem.beneficiaryId())
                        .beneficiaryName(paymentItem.beneficiaryName())
                        .beneficiaryIBAN(paymentItem.beneficiaryIBAN())
                        .amount(paymentItem.amount())
                        .currency(request.currency())
                        .paymentReference(paymentItem.paymentReference())
                        .status(BatchStatus.PENDING)  // Individual payment status
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .toList();

        paymentTransactionRepository.saveAll(paymentEntities);
        log.info("Created {} individual payments for batch: {}", paymentEntities.size(), savedBatch.getBatchId());

        // Step 5: Store idempotency key mapping
        idempotencyService.storeIdempotencyMapping(request.idempotencyKey(), savedBatch.getBatchId());

        // Return response
        return new PaymentBatchCreateResponse(
                savedBatch.getBatchId(),
                savedBatch.getStatus(),
                savedBatch.getCreatedAt(),
                "/api/v1/batch-payment/" + savedBatch.getBatchId() + "/status",
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
}
