package com.vishal.apiflow.analyzer.model;

public class MetricAggregator {
    public long count = 0;
    public double totalDuration = 0;
    public long errorCount = 0;

    public MetricAggregator() {}

    public MetricAggregator add(EnrichedSpan span) {
        this.count++;
        this.totalDuration += span.getDurationMs();
        if (span.isError()) {
            this.errorCount++;
        }
        return this;
    }
}