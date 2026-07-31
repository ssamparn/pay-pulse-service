package com.paypulse.platform.infrastructure.soap;

import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;

import java.util.List;

public record HistoricalSoapBatchSnapshot(
        PaymentBatchEntity batch,
        List<PaymentTransactionEntity> transactions
) {
}

