package com.paypulse.platform.infrastructure.service;

import com.paypulse.platform.infrastructure.soap.HistoricalBatchSoapClient;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalBatchSoapService {

    private final HistoricalBatchSoapClient historicalBatchSoapClient;

    /**
     * Fetches historical batches from external SOAP API.
     */
    public List<PaymentBatchEntity> fetchHistoricalBatches(LocalDate fromDate, LocalDate toDate) {
        log.info("Calling SOAP historical service. From: {}, To: {}", fromDate, toDate);

        try {
            List<PaymentBatchEntity> batches = historicalBatchSoapClient.getHistoricalBatches(fromDate, toDate);
            log.info("Retrieved {} batches from SOAP service", batches.size());
            return batches;
        } catch (Exception e) {
            log.error("Failed to fetch historical batches from SOAP service", e);
            throw new RuntimeException("Failed to fetch historical data", e);
        }
    }
}
