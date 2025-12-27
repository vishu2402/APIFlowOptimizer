package com.vishal.apiflow.analyzer.controller;

import com.vishal.apiflow.analyzer.service.AlertPublisher;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;
    private final AlertPublisher alertPublisher;

    public DashboardController(JdbcTemplate jdbcTemplate, AlertPublisher alertPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.alertPublisher = alertPublisher;
    }

    @QueryMapping
    public List<Map<String, Object>> getSlowestQueries(@Argument int limit) {
        String sql = """
            SELECT 
                trace_id as "traceId", 
                service_name as "serviceName", 
                operation_name as "operationName", 
                duration_ms as "durationMs",
                (payload->'attributes') as "sqlText" -- Simplified for now to avoid null errors
            FROM traces 
            WHERE duration_ms > 0 
            ORDER BY duration_ms DESC 
            LIMIT ?
        """;

        return jdbcTemplate.queryForList(sql, limit);
    }

    @QueryMapping
    public Integer getAnomalyCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM traces WHERE duration_ms > 300",
                Integer.class
        );
    }

    @PostMapping("/debug/publish")
    public ResponseEntity<String> debugPublish(){
        alertPublisher.publish("test-svc", 123.45, "SELECT 1", "auto-fix");
        return ResponseEntity.ok("ok");
    }

    @SubscriptionMapping
    public Flux<AlertPublisher.AnomalyAlert> liveAlerts() {
        return alertPublisher.getAlertStream();
    }
}