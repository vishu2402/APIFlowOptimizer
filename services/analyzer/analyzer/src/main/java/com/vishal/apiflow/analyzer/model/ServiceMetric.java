package com.vishal.apiflow.analyzer.model;

public class ServiceMetric {
    private String serviceName;
    private long windowStart;
    private double avgLatency;
    private long errorCount;

    public ServiceMetric() {}

    public ServiceMetric(String serviceName, long windowStart, double avgLatency, long errorCount) {
        this.serviceName = serviceName;
        this.windowStart = windowStart;
        this.avgLatency = avgLatency;
        this.errorCount = errorCount;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public long getWindowStart() { return windowStart; }
    public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
    public double getAvgLatency() { return avgLatency; }
    public void setAvgLatency(double avgLatency) { this.avgLatency = avgLatency; }
    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }

    @Override
    public String toString() {
        return String.format("[%s] Avg: %.2fms | Errors: %d", serviceName, avgLatency, errorCount);
    }
}