package com.paypulse.platform.historicalbatchesmockwebservice.soap.model.rpy;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class HistoricalBatchRpy {

    @XmlElement(required = true)
    private String batchId;

    @XmlElement(required = true)
    private String externalBatchId;

    @XmlElement(required = true)
    private String merchantId;

    @XmlElement(required = true)
    private String customerId;

    @XmlElement(required = true)
    private String status;

    @XmlElement(required = true)
    private String totalAmount;

    @XmlElement(required = true)
    private String currency;

    @XmlElement(required = true)
    private String paymentMethod;

    @XmlElement(required = true)
    private int paymentCount;

    @XmlElement(required = true)
    private int successfulPayments;

    @XmlElement(required = true)
    private int failedPayments;

    @XmlElement(required = true)
    private int pendingPayments;

    @XmlElement(required = true)
    private int progressPercentage;

    @XmlElement(required = true)
    private String createdAt;

    @XmlElement
    private String completedAt;

    @XmlElement(required = true)
    private String lastUpdatedAt;

    @XmlElement
    private String lastErrorMessage;

    @XmlElement(required = true)
    private boolean stale;

    @XmlElementWrapper(name = "transactions")
    @XmlElement(name = "transaction")
    private List<HistoricalPaymentTxnRpy> transactions = new ArrayList<>();

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    public int getSuccessfulPayments() {
        return successfulPayments;
    }

    public void setSuccessfulPayments(int successfulPayments) {
        this.successfulPayments = successfulPayments;
    }

    public int getFailedPayments() {
        return failedPayments;
    }

    public void setFailedPayments(int failedPayments) {
        this.failedPayments = failedPayments;
    }

    public int getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(int pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(String lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public List<HistoricalPaymentTxnRpy> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<HistoricalPaymentTxnRpy> transactions) {
        this.transactions = transactions;
    }
}

