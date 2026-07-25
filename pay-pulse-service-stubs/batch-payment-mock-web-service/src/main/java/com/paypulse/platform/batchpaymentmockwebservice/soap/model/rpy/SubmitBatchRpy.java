package com.paypulse.platform.batchpaymentmockwebservice.soap.model.rpy;

import com.paypulse.platform.batchpaymentmockwebservice.soap.model.SoapContractConstants;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "SubmitBatchRpy", namespace = SoapContractConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class SubmitBatchRpy {

    @XmlElement(required = true)
    private String batchId;

    @XmlElement(required = true)
    private String processedAt;

    @XmlElementWrapper(name = "transactions")
    @XmlElement(name = "transaction")
    private List<SubmitPaymentTxnRpy> transactions = new ArrayList<>();

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }

    public List<SubmitPaymentTxnRpy> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<SubmitPaymentTxnRpy> transactions) {
        this.transactions = transactions;
    }
}

