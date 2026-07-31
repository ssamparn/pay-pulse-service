package com.paypulse.platform.infrastructure.soap;

import com.paypulse.platform.infrastructure.soap.client.AbstractSpringWsSoapClient;
import com.paypulse.platform.infrastructure.soap.mapper.HistoricalSoapRequestMapper;
import com.paypulse.platform.infrastructure.soap.mapper.HistoricalSoapResponseMapper;
import com.paypulse.platform.infrastructure.soap.model.req.RetrieveHistoricalBatchesReq;
import com.paypulse.platform.infrastructure.soap.model.rpy.RetrieveHistoricalBatchesRpy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.oxm.Marshaller;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class HistoricalBatchSoapClient extends AbstractSpringWsSoapClient<RetrieveHistoricalBatchesReq, RetrieveHistoricalBatchesRpy> {

    private final HistoricalSoapRequestMapper historicalSoapRequestMapper;
    private final HistoricalSoapResponseMapper historicalSoapResponseMapper;

    @Value("${paypulse.soap.historical-batch.uri:http://localhost:7071/ws}")
    private String historicalBatchSoapEndpoint;

    public HistoricalBatchSoapClient(
            HistoricalSoapRequestMapper historicalSoapRequestMapper,
            HistoricalSoapResponseMapper historicalSoapResponseMapper,
            Marshaller batchSoapMarshaller
    ) {
        super(batchSoapMarshaller);
        this.historicalSoapRequestMapper = historicalSoapRequestMapper;
        this.historicalSoapResponseMapper = historicalSoapResponseMapper;
    }

    @PostConstruct
    void initializeGateway() {
        setDefaultUri(historicalBatchSoapEndpoint);
    }

    public List<HistoricalSoapBatchSnapshot> getHistoricalBatches(
            LocalDate fromDate,
            LocalDate toDate,
            String period,
            Integer page,
            Integer pageSize,
            boolean includeTransactions
    ) {
        RetrieveHistoricalBatchesReq request = historicalSoapRequestMapper.toRequest(
                fromDate,
                toDate,
                period,
                page,
                pageSize,
                includeTransactions
        );

        RetrieveHistoricalBatchesRpy response = send(request);
        int importedBatches = response == null || response.getBatches() == null ? 0 : response.getBatches().size();
        log.info("Historical SOAP returned {} batches for range {} to {}", importedBatches, fromDate, toDate);
        return historicalSoapResponseMapper.toSnapshots(response);
    }

    @Override
    protected RetrieveHistoricalBatchesRpy mapResponse(Object response) {
        if (response instanceof RetrieveHistoricalBatchesRpy retrieveHistoricalBatchesRpy) {
            return retrieveHistoricalBatchesRpy;
        }
        throw new IllegalStateException("Unexpected SOAP response type: " + response);
    }
}
