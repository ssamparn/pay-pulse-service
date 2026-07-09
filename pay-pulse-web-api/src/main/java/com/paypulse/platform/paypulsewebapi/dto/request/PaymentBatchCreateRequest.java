package com.paypulse.platform.paypulsewebapi.dto.request;

import com.paypulse.platform.paypulsewebapi.dto.common.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating a new payment batch request.
 *
 */
public record PaymentBatchCreateRequest(
        @NotBlank(message = "Merchant ID is required")
        String merchantId,

        @NotBlank(message = "Customer ID is required")
        String customerId,

        @NotBlank(message = "Batch Id is required")
        String batchId,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.01", message = "Total amount must be at least 0.01")
        BigDecimal totalAmount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
        String currency,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @FutureOrPresent(message = "Execution date must be today or in the future")
        LocalDate executionDate,

        @NotEmpty(message = "At least one payment is required")
        @Size(min = 1, max = 1000, message = "A batch may contain at most 1000 payments")
        List<@Valid PaymentItemRequest> payments
) {
    public record PaymentItemRequest(
            @NotBlank(message = "Beneficiary ID is required")
            String beneficiaryId,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            BigDecimal amount,

            @Size(max = 255)
            String paymentReference) {
    }
}

