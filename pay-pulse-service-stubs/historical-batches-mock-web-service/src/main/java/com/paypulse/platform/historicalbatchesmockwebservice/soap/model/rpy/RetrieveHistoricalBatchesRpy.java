package com.paypulse.platform.historicalbatchesmockwebservice.soap.model.rpy;

import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.SoapContractConstants;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "RetrieveHistoricalBatchesRpy", namespace = SoapContractConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class RetrieveHistoricalBatchesRpy {

    @XmlElement(required = true)
    private String requestId;

    @XmlElement(required = true)
    private String generatedAt;

    @XmlElement(required = true)
    private String fromDate;

    @XmlElement(required = true)
    private String toDate;

    @XmlElement(required = true)
    private int currentPage;

    @XmlElement(required = true)
    private int pageSize;

    @XmlElement(required = true)
    private int totalPages;

    @XmlElement(required = true)
    private int totalBatches;

    @XmlElement(required = true)
    private String sourceSystem;

    @XmlElement
    private String staleAsOf;

    @XmlElementWrapper(name = "batches")
    @XmlElement(name = "batch")
    private List<HistoricalBatchRpy> batches = new ArrayList<>();

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
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

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalBatches() {
        return totalBatches;
    }

    public void setTotalBatches(int totalBatches) {
        this.totalBatches = totalBatches;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getStaleAsOf() {
        return staleAsOf;
    }

    public void setStaleAsOf(String staleAsOf) {
        this.staleAsOf = staleAsOf;
    }

    public List<HistoricalBatchRpy> getBatches() {
        return batches;
    }

    public void setBatches(List<HistoricalBatchRpy> batches) {
        this.batches = batches;
    }
}

