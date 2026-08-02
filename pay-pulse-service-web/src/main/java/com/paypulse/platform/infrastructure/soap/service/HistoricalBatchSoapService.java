package com.paypulse.platform.infrastructure.soap.service;

import com.paypulse.platform.infrastructure.soap.client.impl.HistoricalBatchSoapClient;
import com.paypulse.platform.infrastructure.soap.HistoricalSoapBatchSnapshot;
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
    public List<HistoricalSoapBatchSnapshot> fetchHistoricalBatches(
            LocalDate fromDate,
            LocalDate toDate,
            String period,
            Integer page,
            Integer pageSize,
            boolean includeTransactions
    ) {
        log.info("Calling SOAP historical service. From: {}, To: {}, Period: {}, Page: {}, PageSize: {}",
                fromDate, toDate, period, page, pageSize);

        try {
            List<HistoricalSoapBatchSnapshot> batches = historicalBatchSoapClient.getHistoricalBatches(
                    fromDate,
                    toDate,
                    period,
                    page,
                    pageSize,
                    includeTransactions
            );
            log.info("Retrieved {} batches from SOAP service", batches.size());
            return batches;
        } catch (Exception e) {
            log.error("Failed to fetch historical batches from SOAP service", e);
            throw new RuntimeException("Failed to fetch historical data", e);
        }
    }
}
