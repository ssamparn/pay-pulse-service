package com.paypulse.platform.paypulsewebapi.dto;

import java.math.BigDecimal;

public record PaymentBatchCreateRequest(
        String clientReference,
        int paymentCount,
        BigDecimal totalAmount,
        String currency
) {
}

