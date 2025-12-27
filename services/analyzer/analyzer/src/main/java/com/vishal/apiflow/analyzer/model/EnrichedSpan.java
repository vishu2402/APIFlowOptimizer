package com.vishal.apiflow.analyzer.model;

public class EnrichedSpan {
    private String traceId;
    private String serviceName;
    private String operationName;
    private double durationMs;
    private String statusCode;
    private boolean isError;

    public EnrichedSpan() {}

    public EnrichedSpan(String traceId, String serviceName, String operationName, double durationMs, String statusCode, boolean isError) {
        this.traceId = traceId;
        this.serviceName = serviceName;
        this.operationName = operationName;
        this.durationMs = durationMs;
        this.statusCode = statusCode;
        this.isError = isError;
    }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getOperationName() { return operationName; }
    public void setOperationName(String operationName) { this.operationName = operationName; }
    public double getDurationMs() { return durationMs; }
    public void setDurationMs(double durationMs) { this.durationMs = durationMs; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public boolean isError() { return isError; }
    public void setError(boolean error) { isError = error; }

    @Override
    public String toString() {
        return "EnrichedSpan{service='" + serviceName + "', op='" + operationName + "', ms=" + durationMs + "}";
    }
}