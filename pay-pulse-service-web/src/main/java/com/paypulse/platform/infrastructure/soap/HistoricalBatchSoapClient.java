package com.paypulse.platform.infrastructure.soap;

import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class HistoricalBatchSoapClient {

    public List<PaymentBatchEntity> getHistoricalBatches(LocalDate fromDate, LocalDate toDate) {

        return null;
    }
}
