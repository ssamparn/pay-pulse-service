package com.paypulse.platform.persistence.service;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final Map<String, PendingSubmission> pendingSubmissions = new ConcurrentHashMap<>();

    public PaymentBatchEntity getExistingBatch(String idempotencyKey) {
        PaymentBatchEntity persistedBatch = paymentBatchRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (persistedBatch != null) {
            return persistedBatch;
        }

        PendingSubmission pendingSubmission = pendingSubmissions.get(idempotencyKey);
        if (pendingSubmission == null) {
            return null;
        }

        return PaymentBatchEntity.create()
                .batchId(pendingSubmission.batchId())
                .status(BatchStatus.PENDING)
                .createdAt(pendingSubmission.createdAt())
                .updatedAt(pendingSubmission.createdAt())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    public PaymentBatchEntity reserveSubmission(String idempotencyKey, String batchId, LocalDateTime createdAt) {
        PaymentBatchEntity existingBatch = getExistingBatch(idempotencyKey);
        if (existingBatch != null) {
            return existingBatch;
        }

        PendingSubmission newSubmission = new PendingSubmission(batchId, createdAt);
        PendingSubmission existingSubmission = pendingSubmissions.putIfAbsent(idempotencyKey, newSubmission);
        if (existingSubmission == null) {
            log.debug("Reserved pending submission for idempotencyKey={} and batchId={}", idempotencyKey, batchId);
            return null;
        }

        return PaymentBatchEntity.create()
                .batchId(existingSubmission.batchId())
                .status(BatchStatus.PENDING)
                .createdAt(existingSubmission.createdAt())
                .updatedAt(existingSubmission.createdAt())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    public void storeIdempotencyMapping(String idempotencyKey, String batchId) {
        pendingSubmissions.remove(idempotencyKey);
        log.debug("Idempotency mapping persisted for idempotencyKey={} and batchId={}", idempotencyKey, batchId);
    }

    public void clearPendingSubmission(String idempotencyKey) {
        pendingSubmissions.remove(idempotencyKey);
    }

    private record PendingSubmission(String batchId, LocalDateTime createdAt) {
    }

}
