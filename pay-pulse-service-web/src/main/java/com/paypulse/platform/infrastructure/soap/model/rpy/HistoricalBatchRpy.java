package com.paypulse.platform.infrastructure.soap.model.rpy;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class HistoricalBatchRpy {

    @XmlElement
    private String batchId;

    @XmlElement
    private String merchantId;

    @XmlElement
    private String customerId;

    @XmlElement
    private String status;

    @XmlElement
    private String totalAmount;

    @XmlElement
    private String currency;

    @XmlElement
    private String paymentMethod;

    @XmlElement
    private Integer paymentCount;

    @XmlElement
    private Integer successfulPayments;

    @XmlElement
    private Integer failedPayments;

    @XmlElement
    private Integer pendingPayments;

    @XmlElement
    private Integer progressPercentage;

    @XmlElement
    private String createdAt;

    @XmlElement
    private String completedAt;

    @XmlElement
    private String lastUpdatedAt;

    @XmlElement
    private String lastErrorMessage;

    @XmlElement
    private Boolean stale;

    @XmlElementWrapper(name = "transactions")
    @XmlElement(name = "transaction")
    private List<HistoricalPaymentTxnRpy> transactions = new ArrayList<>();

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
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

    public Integer getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(Integer paymentCount) {
        this.paymentCount = paymentCount;
    }

    public Integer getSuccessfulPayments() {
        return successfulPayments;
    }

    public void setSuccessfulPayments(Integer successfulPayments) {
        this.successfulPayments = successfulPayments;
    }

    public Integer getFailedPayments() {
        return failedPayments;
    }

    public void setFailedPayments(Integer failedPayments) {
        this.failedPayments = failedPayments;
    }

    public Integer getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(Integer pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
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

    public Boolean getStale() {
        return stale;
    }

    public void setStale(Boolean stale) {
        this.stale = stale;
    }

    public List<HistoricalPaymentTxnRpy> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<HistoricalPaymentTxnRpy> transactions) {
        this.transactions = transactions;
    }
}

