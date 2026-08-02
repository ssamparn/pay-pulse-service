package com.paypulse.platform.persistence.mapper;

import com.paypulse.platform.common.dto.BatchStatus;
import com.paypulse.platform.web.dto.request.PaymentBatchCreateRequest;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentBatchEntityMapper {

    public PaymentBatchEntity toPaymentBatchEntity(PaymentBatchCreateRequest request, LocalDateTime acceptedAt) {
        int totalTransactions = request.payments().size();

        return PaymentBatchEntity.create()
                .batchId(request.batchId())
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .status(BatchStatus.PENDING)
                .totalAmount(request.totalAmount())
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .executionDate(request.executionDate())
                .batchDescription(request.batchDescription())
                .requestedBy(request.requestedBy())
                .paymentsCount(totalTransactions)
                .totalTransactions(totalTransactions)
                .successfulTransactions(0)
                .failedTransactions(0)
                .pendingTransactions(totalTransactions)
                .progressPercentage(0)
                .recoveryAttemptCount(0)
                .createdAt(acceptedAt)
                .updatedAt(acceptedAt)
                .build();
    }
}

