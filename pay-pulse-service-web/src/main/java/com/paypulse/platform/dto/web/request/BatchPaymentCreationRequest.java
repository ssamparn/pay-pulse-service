package com.paypulse.platform.dto.web.request;

import com.paypulse.platform.dto.common.PaymentMethod;
import com.paypulse.platform.dto.web.validator.ValidBatchTotal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
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
 * Request DTO for creating a new payment batch request.
 *
 */
@ValidBatchTotal
public record BatchPaymentCreationRequest(
        @NotBlank(message = "Batch Id is required")
        String batchId,

        @NotBlank(message = "Merchant ID is required")
        String merchantId,

        @NotBlank(message = "Customer ID is required")
        String customerId,

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.01", message = "Total amount must be at least 0.01")
        BigDecimal totalAmount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
        String currency,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Execution date is required")
        @FutureOrPresent(message = "Execution date must be today or in the future")
        LocalDate executionDate,

        @NotBlank(message = "Batch description is required")
        @Size(min = 1, max = 500, message = "Batch description must be between 1 and 500 characters")
        String batchDescription,

        @NotBlank(message = "Requested by is required")
        @Email(message = "Requested by must be a valid email address")
        String requestedBy,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotEmpty(message = "At least one payment is required")
        @Size(min = 1, max = 1000, message = "A batch may contain at most 1000 payments")
        List<@Valid PaymentItemRequest> payments
) {
    public record PaymentItemRequest(

            @NotBlank(message = "Payment ID is required")
            String paymentId,

            @NotBlank(message = "Beneficiary ID is required")
            String beneficiaryId,

            @NotBlank(message = "Beneficiary name is required")
            @Size(min = 1, max = 255, message = "Beneficiary name must be between 1 and 255 characters")
            String beneficiaryName,

            @NotBlank(message = "Beneficiary IBAN is required")
            @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$", message = "Invalid IBAN format (must be 15-34 characters)")
            String beneficiaryIBAN,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            BigDecimal amount,

            @NotBlank(message = "Payment reference is required")
            @Size(min = 1, max = 255, message = "Payment reference must be between 1 and 255 characters")
            String paymentReference,

            @NotBlank(message = "Description is required")
            @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
            String description
    ) {
    }
}

