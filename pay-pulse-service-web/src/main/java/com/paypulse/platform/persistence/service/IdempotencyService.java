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

    public PaymentBatchEntity getPersistedBatch(String idempotencyKey) {
        return paymentBatchRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    public PaymentBatchEntity reserveSubmission(String idempotencyKey, String batchId, LocalDateTime createdAt) {
        PaymentBatchEntity persistedBatch = getPersistedBatch(idempotencyKey);
        if (persistedBatch != null) {
            return persistedBatch;
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
