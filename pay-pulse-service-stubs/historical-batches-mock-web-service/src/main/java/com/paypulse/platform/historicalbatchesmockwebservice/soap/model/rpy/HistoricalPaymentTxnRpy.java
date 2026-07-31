package com.paypulse.platform.historicalbatchesmockwebservice.soap.model.rpy;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class HistoricalPaymentTxnRpy {

    @XmlElement(required = true)
    private String paymentId;

    @XmlElement(required = true)
    private String externalPaymentId;

    @XmlElement(required = true)
    private String beneficiaryId;

    @XmlElement(required = true)
    private String beneficiaryName;

    @XmlElement(required = true)
    private String beneficiaryIbanMasked;

    @XmlElement(required = true)
    private String amount;

    @XmlElement(required = true)
    private String currency;

    @XmlElement(required = true)
    private String paymentReference;

    @XmlElement(required = true)
    private String status;

    @XmlElement(required = true)
    private boolean retryable;

    @XmlElement
    private String failureReason;

    @XmlElement
    private String processedAt;

    @XmlElement(required = true)
    private String updatedAt;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public void setExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
    }

    public String getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(String beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryIbanMasked() {
        return beneficiaryIbanMasked;
    }

    public void setBeneficiaryIbanMasked(String beneficiaryIbanMasked) {
        this.beneficiaryIbanMasked = beneficiaryIbanMasked;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

