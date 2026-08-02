package com.paypulse.platform.mapper;

import com.paypulse.platform.common.dto.BatchStatus;
import com.paypulse.platform.web.dto.response.PaymentBatchCreateResponse;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentBatchCreateResponseMapper {

    public PaymentBatchCreateResponse toAcceptedResponse(String batchId, LocalDateTime acceptedAt) {
        return new PaymentBatchCreateResponse(
                batchId,
                BatchStatus.PENDING,
                acceptedAt,
                statusUrl(batchId),
                false
        );
    }

    public PaymentBatchCreateResponse toDuplicateResponse(PaymentBatchEntity existingBatch) {
        return new PaymentBatchCreateResponse(
                existingBatch.getBatchId(),
                existingBatch.getStatus(),
                existingBatch.getCreatedAt(),
                statusUrl(existingBatch.getBatchId()),
                true
        );
    }

    private String statusUrl(String batchId) {
        return "/api/v1/batch-payment/" + batchId + "/status";
    }
}