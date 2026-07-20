package com.paypulse.platform.persistence.entity;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.common.PaymentMethod;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment_batch",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_batch_idempotency_key", columnNames = "idempotency_key"),
                @UniqueConstraint(name = "uk_payment_batch_external_batch_id", columnNames = "external_batch_id")
        },
        indexes = {
                @Index(name = "idx_payment_batch_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_payment_batch_created_at", columnList = "created_at"),
                @Index(name = "idx_payment_batch_merchant_customer", columnList = "merchant_id, customer_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "create")
public class PaymentBatchEntity {

    @Id
    @Column(name = "batch_id", nullable = false, length = 64)
    private String batchId;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "external_batch_id", nullable = false, length = 64)
    private String externalBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BatchStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Column(name = "batch_description", nullable = false, length = 500)
    private String batchDescription;

    @Column(name = "requested_by", nullable = false, length = 320)
    private String requestedBy;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "payments_count", nullable = false)
    private Integer paymentsCount;

    @Column(name = "total_transactions", nullable = false)
    private Integer totalTransactions;

    @Column(name = "successful_transactions", nullable = false)
    private Integer successfulTransactions;

    @Column(name = "failed_transactions", nullable = false)
    private Integer failedTransactions;

    @Column(name = "pending_transactions", nullable = false)
    private Integer pendingTransactions;

    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
