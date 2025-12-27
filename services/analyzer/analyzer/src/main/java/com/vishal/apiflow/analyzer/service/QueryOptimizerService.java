package com.vishal.apiflow.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishal.apiflow.analyzer.config.SystemConfig;
import com.vishal.apiflow.analyzer.model.ServiceMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class QueryOptimizerService {

    private final JdbcTemplate jdbcTemplate;
    private final CostEstimator costEstimator;
    private final SystemConfig systemConfig;
    private final AlertPublisher alertPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final double LATENCY_THRESHOLD = 50.0;
    private static final String RL_AGENT_URL = "http://localhost:5000";

    private double lastKnownLatency = 0.0;

    @Autowired
    public QueryOptimizerService(JdbcTemplate jdbcTemplate, CostEstimator costEstimator, SystemConfig systemConfig, AlertPublisher alertPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.costEstimator = costEstimator;
        this.systemConfig = systemConfig;
        this.alertPublisher = alertPublisher; // 2. Initialize it
    }

    @KafkaListener(topics = "ml-features", groupId = "optimizer-group-dashboard-v1")
    public void analyzeMetrics(String message) {
        try {
            ServiceMetric metric = objectMapper.readValue(message, ServiceMetric.class);

            if (!"analyzer-service".equals(metric.getServiceName())) return;

            lastKnownLatency = metric.getAvgLatency();

            if (metric.getAvgLatency() > LATENCY_THRESHOLD) {
                System.out.println("\n==================================================");
                System.out.println("ALERT: High Latency detected for " + metric.getServiceName());
                System.out.println("   Avg Latency: " + String.format("%.2f", metric.getAvgLatency()) + "ms");
                System.out.println("==================================================");

                fixSlowQuery();
            }

        } catch (Exception e) {
            // Handle exceptions
        }
    }

    private void fixSlowQuery() {
        String sqlQuery = """
            SELECT attr -> 'value' ->> 'stringValue' as sql_text
            FROM raw_events r
            JOIN traces t ON r.trace_id = t.trace_id AND r.span_id = t.span_id,
                 jsonb_array_elements(r.payload -> 'attributes') attr 
            WHERE attr ->> 'key' = 'db.statement'
            AND t.duration_ms > 50
            ORDER BY t.start_time DESC 
            LIMIT 1
        """;

        try {
            List<String> results = jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getString("sql_text"));

            if (!results.isEmpty()) {
                String badSql = results.get(0);
                System.out.println("Culprit SQL Found: " + badSql);

                // Trigger the RL workflow
                executeRlAction(badSql);

            } else {
                System.out.println("Alert received, but no slow SQL found in DB history yet.");
            }
        } catch (Exception e) {
            System.err.println("Optimizer Error: " + e.getMessage());
        }
    }

    private void executeRlAction(String sql) {
        // 1. Ask RL Agent
        String queryType = sql.contains("JOIN") ? "heavy_join" : "heavy_db";
        Map<String, String> request = Map.of("query_type", queryType);

        Map response = restTemplate.postForObject(RL_AGENT_URL + "/predict", request, Map.class);
        String action = (String) response.get("suggested_action");

        System.out.println("RL Agent suggests: " + action);

        alertPublisher.publish(
                "analyzer-service",
                lastKnownLatency,
                sql,
                "RL Agent Action: " + action
        );

        applySystemChange(action);
        double reward = calculateReward(action, queryType);

        Map<String, Object> feedback = Map.of("action", action, "reward", reward);
        restTemplate.postForObject(RL_AGENT_URL + "/feedback", feedback, Map.class);

        System.out.println("Feedback Sent. Reward: " + reward);
        System.out.println("==================================================\n");
    }

    private void applySystemChange(String action) {
        SystemConfig.reset();

        if ("ENABLE_CACHE".equals(action)) SystemConfig.CACHE_ENABLED = true;
        if ("APPLY_INDEX".equals(action)) SystemConfig.INDEX_APPLIED = true;
        if ("OPTIMIZE_POOL".equals(action)) SystemConfig.POOL_OPTIMIZED = true;

        System.out.println("Applied Config: " + action);
    }

    private double calculateReward(String action, String queryType) {
        if (queryType.equals("heavy_join") && action.equals("APPLY_INDEX")) return 100.0;
        if (queryType.equals("heavy_db") && action.equals("ENABLE_CACHE")) return 100.0;
        return -10.0;
    }
}