package com.paypulse.platform.service;

import com.paypulse.platform.persistence.entity.PaymentBatch;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    public PaymentBatch getExistingBatch(String idempotencyKey) {
        // Query by idempotency key
        return null;
    }

    public void storeIdempotencyMapping(String idempotencyKey, String batchId) {
        // Store mapping for future lookups
    }

}
