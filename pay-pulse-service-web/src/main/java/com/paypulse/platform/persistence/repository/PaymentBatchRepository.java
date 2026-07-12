package com.paypulse.platform.persistence.repository;

import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentBatchRepository extends JpaRepository<PaymentBatchEntity, String> {

}
