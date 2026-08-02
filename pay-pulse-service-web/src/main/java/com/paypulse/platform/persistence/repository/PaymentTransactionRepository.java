package com.paypulse.platform.persistence.repository;

import com.paypulse.platform.common.dto.BatchStatus;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, String> {
    List<PaymentTransactionEntity> findByBatchId(String batchId);

    long countByBatchIdAndStatus(String batchId, BatchStatus status);

    void deleteByBatchId(String batchId);
}
