package com.paypulse.platform.historicalbatchesmockwebservice.endpoint;

import com.paypulse.platform.historicalbatchesmockwebservice.service.HistoricalBatchScenarioProcessor;
import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.SoapContractConstants;
import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.req.RetrieveHistoricalBatchesReq;
import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.rpy.RetrieveHistoricalBatchesRpy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class HistoricalBatchSoapEndpoint {

    @Autowired
    private HistoricalBatchScenarioProcessor historicalBatchScenarioProcessor;

    @PayloadRoot(namespace = SoapContractConstants.NAMESPACE, localPart = "RetrieveHistoricalBatchesReq")
    @ResponsePayload
    public RetrieveHistoricalBatchesRpy retrieveHistoricalBatches(@RequestPayload RetrieveHistoricalBatchesReq request) {
        return historicalBatchScenarioProcessor.process(request);
    }
}
