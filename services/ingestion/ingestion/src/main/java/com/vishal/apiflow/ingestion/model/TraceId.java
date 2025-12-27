package com.vishal.apiflow.ingestion.model;

import java.io.Serializable;
import java.util.Objects;

public class TraceId implements Serializable {
    private String traceId;
    private String spanId;

    public TraceId() {}
    public TraceId(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceId traceId1 = (TraceId) o;
        return Objects.equals(traceId, traceId1.traceId) && Objects.equals(spanId, traceId1.spanId);
    }
    @Override
    public int hashCode() { return Objects.hash(traceId, spanId); }
}