package com.paypulse.platform.persistence.entity;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.common.PaymentMethod;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "create")
public class PaymentBatchEntity {

    @Id
    private String batchId;
    private String merchantId;
    private String customerId;
    private String externalBatchId;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private BigDecimal totalAmount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private LocalDate executionDate;
    private String batchDescription;
    private String requestedBy;
    private String idempotencyKey;
    private Integer paymentsCount;
    private Integer totalTransactions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
