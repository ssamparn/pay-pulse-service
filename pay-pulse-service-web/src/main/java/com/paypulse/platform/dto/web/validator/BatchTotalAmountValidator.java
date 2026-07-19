package com.paypulse.platform.dto.web.validator;

import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class BatchTotalAmountValidator implements ConstraintValidator<ValidBatchTotal, PaymentBatchCreateRequest> {

    @Override
    public boolean isValid(PaymentBatchCreateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.payments() == null || request.payments().isEmpty()) {
            return true; // Let @NotEmpty and @NotNull handle null/empty cases
        }

        BigDecimal calculatedTotal = request.payments().stream()
                .map(PaymentBatchCreateRequest.PaymentItemRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal batchTotal = request.totalAmount().setScale(2, RoundingMode.HALF_UP);
        boolean isValid = calculatedTotal.compareTo(batchTotal) == 0;

        if (!isValid) {
            log.warn("Batch total mismatch for batchId: {}, expected: {}, calculated: {}",
                    request.batchId(), batchTotal, calculatedTotal);

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            String.format("Total amount %s does not match sum of payments %s",
                                    batchTotal, calculatedTotal))
                    .addConstraintViolation();
        }
        return isValid;
    }
}
