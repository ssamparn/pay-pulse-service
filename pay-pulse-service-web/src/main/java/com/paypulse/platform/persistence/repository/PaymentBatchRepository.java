package com.paypulse.platform.persistence.repository;

import com.paypulse.platform.persistence.entity.PaymentBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentBatchRepository extends JpaRepository<PaymentBatch, Long> {

}
