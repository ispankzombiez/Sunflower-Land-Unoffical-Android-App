package com.sfl.browser.models;

import java.util.List;

public class ProcessedData {
    private List<FarmItem> items;           // All processed farm items, sorted by timestamp
    private long processingTimestamp;       // When this data was processed
    private String processingTime;          // Human-readable processing time

    // Constructor
    public ProcessedData(List<FarmItem> items, long processingTimestamp, String processingTime) {
        this.items = items;
        this.processingTimestamp = processingTimestamp;
        this.processingTime = processingTime;
    }

    // Getters and Setters
    public List<FarmItem> getItems() { return items; }
    public void setItems(List<FarmItem> items) { this.items = items; }

    public long getProcessingTimestamp() { return processingTimestamp; }
    public void setProcessingTimestamp(long processingTimestamp) { this.processingTimestamp = processingTimestamp; }

    public String getProcessingTime() { return processingTime; }
    public void setProcessingTime(String processingTime) { this.processingTime = processingTime; }

    @Override
    public String toString() {
        return "ProcessedData{" +
                "itemCount=" + (items != null ? items.size() : 0) +
                ", processingTime='" + processingTime + '\'' +
                '}';
    }
}
