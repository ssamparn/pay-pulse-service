package com.paypulse.platform.infrastructure.soap.mapper;

import com.paypulse.platform.infrastructure.soap.model.req.RetrieveHistoricalBatchesReq;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HistoricalSoapRequestMapper {

    public RetrieveHistoricalBatchesReq toRequest(
            LocalDate fromDate,
            LocalDate toDate,
            String period,
            Integer page,
            Integer pageSize,
            boolean includeTransactions
    ) {
        RetrieveHistoricalBatchesReq request = new RetrieveHistoricalBatchesReq();
        request.setPeriod(period);
        request.setFromDate(fromDate == null ? null : fromDate.toString());
        request.setToDate(toDate == null ? null : toDate.toString());
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setIncludeTransactions(includeTransactions);
        return request;
    }
}

