package com.paypulse.platform.persistence.service;

import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    public PaymentBatchEntity getExistingBatch(String idempotencyKey) {
        // Query by idempotency key
        return null;
    }

    public void storeIdempotencyMapping(String idempotencyKey, String batchId) {
        // Store mapping for future lookups
    }

}
