package com.paypulse.platform.persistence.entity;

import com.paypulse.platform.dto.common.BatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment_transaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_transaction_batch_external_payment", columnNames = {"batch_id", "external_payment_id"})
        },
        indexes = {
                @Index(name = "idx_payment_transaction_batch_id", columnList = "batch_id"),
                @Index(name = "idx_payment_transaction_batch_status", columnList = "batch_id, status"),
                @Index(name = "idx_payment_transaction_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "create")
public class PaymentTransactionEntity {

    @Id
    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Column(name = "beneficiary_id", nullable = false, length = 64)
    private String beneficiaryId;

    @Column(name = "batch_id", nullable = false, length = 64)
    private String batchId;

    @Column(name = "beneficiary_name", nullable = false, length = 255)
    private String beneficiaryName;

    @Column(name = "beneficiary_iban", nullable = false, length = 34)
    private String beneficiaryIBAN;

    @Column(name = "external_payment_id", nullable = false, length = 64)
    private String externalPaymentId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_reference", nullable = false, length = 255)
    private String paymentReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retryable", nullable = false)
    private boolean retryable; // Whether this failure can be retried

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BatchStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

