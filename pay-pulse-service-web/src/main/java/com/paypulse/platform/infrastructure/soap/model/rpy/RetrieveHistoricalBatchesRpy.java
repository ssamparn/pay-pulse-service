package com.paypulse.platform.infrastructure.soap.model.rpy;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "RetrieveHistoricalBatchesRpy", namespace = "http://paypulse.platform.com/soap/historical-batches")
@XmlAccessorType(XmlAccessType.FIELD)
public class RetrieveHistoricalBatchesRpy {

    @XmlElement
    private String requestId;

    @XmlElement
    private String generatedAt;

    @XmlElement
    private String fromDate;

    @XmlElement
    private String toDate;

    @XmlElement
    private Integer currentPage;

    @XmlElement
    private Integer pageSize;

    @XmlElement
    private Integer totalPages;

    @XmlElement
    private Integer totalBatches;

    @XmlElement
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

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getTotalBatches() {
        return totalBatches;
    }

    public void setTotalBatches(Integer totalBatches) {
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
