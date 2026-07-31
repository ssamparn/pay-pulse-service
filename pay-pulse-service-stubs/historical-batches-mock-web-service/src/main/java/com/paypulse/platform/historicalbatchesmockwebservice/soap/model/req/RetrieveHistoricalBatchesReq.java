package com.paypulse.platform.historicalbatchesmockwebservice.soap.model.req;

import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.SoapContractConstants;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RetrieveHistoricalBatchesReq", namespace = SoapContractConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class RetrieveHistoricalBatchesReq {

    @XmlElement
    private String period;

    @XmlElement
    private String fromDate;

    @XmlElement
    private String toDate;

    @XmlElement
    private String merchantId;

    @XmlElement
    private String customerId;

    @XmlElement
    private Integer page;

    @XmlElement
    private Integer pageSize;

    @XmlElement
    private Boolean includeTransactions;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
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

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Boolean getIncludeTransactions() {
        return includeTransactions;
    }

    public void setIncludeTransactions(Boolean includeTransactions) {
        this.includeTransactions = includeTransactions;
    }
}

