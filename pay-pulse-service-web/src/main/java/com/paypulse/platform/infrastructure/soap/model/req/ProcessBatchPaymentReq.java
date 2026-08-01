package com.paypulse.platform.infrastructure.soap.model.req;

import com.paypulse.platform.infrastructure.soap.model.SoapContractConstants;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ProcessBatchPaymentReq", namespace = SoapContractConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessBatchPaymentReq {

    @XmlElement(required = true)
    private String batchId;

    @XmlElement(required = true)
    private String externalBatchId;

    @XmlElement(required = true)
    private String merchantId;

    @XmlElement(required = true)
    private String customerId;

    @XmlElement(required = true)
    private String paymentMethod;

    @XmlElement(required = true)
    private String executionDate;

    @XmlElement(required = true)
    private String totalAmount;

    @XmlElement(required = true)
    private String currency;

    @XmlElement(required = true)
    private String requestedBy;

    @XmlElementWrapper(name = "transactions")
    @XmlElement(name = "transaction")
    private List<SubmitPaymentTxnReq> transactions = new ArrayList<>();

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getExternalBatchId() {
        return externalBatchId;
    }

    public void setExternalBatchId(String externalBatchId) {
        this.externalBatchId = externalBatchId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(String executionDate) {
        this.executionDate = executionDate;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public List<SubmitPaymentTxnReq> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<SubmitPaymentTxnReq> transactions) {
        this.transactions = transactions;
    }
}


