package com.paypulse.platform.persistence.entity;

import com.paypulse.platform.dto.common.BatchStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "create")
public class Payment {

    @Id
    private String paymentId;

    private String externalPaymentId;
    private String batchId;
    private String beneficiaryId;
    private String beneficiaryName;
    private String beneficiaryIBAN;
    private BigDecimal amount;
    private String currency;
    private String paymentReference;

    private String description;
    private String failureReason;
    private boolean retryable; // Whether this failure can be retried
    @Enumerated(EnumType.STRING)

    private BatchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
