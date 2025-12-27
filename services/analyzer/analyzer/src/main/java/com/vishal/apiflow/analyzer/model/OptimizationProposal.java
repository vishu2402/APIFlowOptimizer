package com.vishal.apiflow.analyzer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_proposals")
public class OptimizationProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String traceId;
    private String serviceName;

    @Column(columnDefinition = "TEXT")
    private String culpritSql;

    @Column(columnDefinition = "TEXT")
    private String aiSuggestion;

    private String status;
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public OptimizationProposal() {}

    public OptimizationProposal(String traceId, String serviceName, String culpritSql, String aiSuggestion) {
        this.traceId = traceId;
        this.serviceName = serviceName;
        this.culpritSql = culpritSql;
        this.aiSuggestion = aiSuggestion;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTraceId() { return traceId; }
    public String getServiceName() { return serviceName; }
    public String getCulpritSql() { return culpritSql; }
    public String getAiSuggestion() { return aiSuggestion; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
