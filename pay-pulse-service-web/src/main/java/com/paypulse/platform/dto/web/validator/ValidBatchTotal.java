package com.paypulse.platform.dto.web.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = BatchTotalAmountValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBatchTotal {
    String message() default "Total amount must match the sum of all payments";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
