package com.paypulse.platform.persistence.repository;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PaymentBatchRepository extends JpaRepository<PaymentBatchEntity, String> {

	Optional<PaymentBatchEntity> findByBatchId(String batchId);

	List<PaymentBatchEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime fromInclusive, LocalDateTime toInclusive);

	@Query("select batch.batchId from PaymentBatchEntity batch where batch.status = :status order by batch.createdAt asc")
	List<String> findBatchIdsByStatusOrderByCreatedAtAsc(@Param("status") BatchStatus status, Pageable pageable);

	@Query("""
			select batch.batchId
			  from PaymentBatchEntity batch
			 where batch.status = :status
			   and batch.updatedAt < :updatedBefore
			 order by batch.updatedAt asc
			""")
	List<String> findBatchIdsByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
			@Param("status") BatchStatus status,
			@Param("updatedBefore") LocalDateTime updatedBefore,
			Pageable pageable
	);

	@Modifying
	@Transactional
	@Query("""
			update PaymentBatchEntity batch
			   set batch.status = :targetStatus,
				   batch.updatedAt = :updatedAt
			 where batch.batchId = :batchId
			   and batch.status = :expectedStatus
			""")
	int updateBatchStatusIfCurrentStatusMatches(
			@Param("batchId") String batchId,
			@Param("expectedStatus") BatchStatus expectedStatus,
			@Param("targetStatus") BatchStatus targetStatus,
			@Param("updatedAt") LocalDateTime updatedAt
	);

}
