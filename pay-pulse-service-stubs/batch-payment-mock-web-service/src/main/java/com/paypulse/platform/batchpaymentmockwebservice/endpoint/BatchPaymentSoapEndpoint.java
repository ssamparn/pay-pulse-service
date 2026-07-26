package com.paypulse.platform.batchpaymentmockwebservice.endpoint;

import com.paypulse.platform.batchpaymentmockwebservice.soap.model.SoapContractConstants;
import com.paypulse.platform.batchpaymentmockwebservice.soap.model.req.ProcessBatchPaymentReq;
import com.paypulse.platform.batchpaymentmockwebservice.soap.model.rpy.ProcessBatchPaymentRpy;
import com.paypulse.platform.batchpaymentmockwebservice.service.BatchPaymentScenarioProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Slf4j
@Endpoint
@RequiredArgsConstructor
public class BatchPaymentSoapEndpoint {

    private final BatchPaymentScenarioProcessor batchPaymentScenarioProcessor;

    @PayloadRoot(namespace = SoapContractConstants.NAMESPACE, localPart = "ProcessBatchPaymentReq")
    @ResponsePayload
    public ProcessBatchPaymentRpy processBatchPayment(@RequestPayload ProcessBatchPaymentReq request) {
        ProcessBatchPaymentRpy response = batchPaymentScenarioProcessor.processBatch(request);

        log.info("SOAP stub processed batchId={} and returned {} transaction outcomes",
                request.getBatchId(),
                response.getTransactions().size());
        return response;
    }
}

