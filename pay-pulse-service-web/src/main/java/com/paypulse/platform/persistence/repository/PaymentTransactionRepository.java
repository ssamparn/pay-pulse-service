package com.paypulse.platform.persistence.repository;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.persistence.entity.PaymentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentEntity, String> {
    List<PaymentEntity> findByBatchId(String batchId);

    long countByBatchIdAndStatus(String batchId, BatchStatus status);
}
