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

    public PaymentBatchEntity getPersistedBatch(String batchId) {
        return paymentBatchRepository.findByBatchId(batchId).orElse(null);
    }

    public PaymentBatchEntity reserveSubmission(String batchId, LocalDateTime createdAt) {
        PaymentBatchEntity persistedBatch = getPersistedBatch(batchId);
        if (persistedBatch != null) {
            return persistedBatch;
        }

        PendingSubmission newSubmission = new PendingSubmission(batchId, createdAt);
        PendingSubmission existingSubmission = pendingSubmissions.putIfAbsent(batchId, newSubmission);
        if (existingSubmission == null) {
            log.debug("Reserved pending submission for batchId={}", batchId);
            return null;
        }

        return PaymentBatchEntity.create()
                .batchId(existingSubmission.batchId())
                .status(BatchStatus.PENDING)
                .createdAt(existingSubmission.createdAt())
                .updatedAt(existingSubmission.createdAt())
                .build();
    }

    public void storeIdempotencyMapping(String batchId) {
        pendingSubmissions.remove(batchId);
        log.debug("Idempotency mapping persisted for batchId={}", batchId);
    }

    public void clearPendingSubmission(String batchId) {
        pendingSubmissions.remove(batchId);
    }

    private record PendingSubmission(String batchId, LocalDateTime createdAt) {
    }

}
