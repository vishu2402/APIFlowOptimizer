package com.vishal.apiflow.analyzer.controller;

import com.vishal.apiflow.analyzer.config.SystemConfig;
import com.vishal.apiflow.analyzer.service.AlertPublisher;
import com.vishal.apiflow.analyzer.service.GovernanceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Random;

@RestController
public class SimulationController {

    private final JdbcTemplate jdbcTemplate;
    private final AlertPublisher alertPublisher;
    private final GovernanceService governanceService;
    private final Random random = new Random();

    // Inject all required services
    public SimulationController(JdbcTemplate jdbcTemplate,
                                AlertPublisher alertPublisher,
                                GovernanceService governanceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.alertPublisher = alertPublisher;
        this.governanceService = governanceService;
    }

    // ==========================================
    //  EXISTING REST ENDPOINT (DB Simulation)
    // ==========================================
    @GetMapping("/analyze-work")
    public String doWork(@RequestParam(defaultValue = "random") String type) {
        long start = System.currentTimeMillis();
        String operation = type;

        try {
            if ("heavy_join".equals(type)) {
                if (SystemConfig.INDEX_APPLIED) {
                    jdbcTemplate.queryForList("SELECT 1");
                    operation = "Optimized Join (Indexed)";
                } else {
                    String sql = "SELECT u.region, SUM(o.amount) FROM users u JOIN orders o ON u.id = o.user_id GROUP BY u.region";
                    jdbcTemplate.queryForList(sql);
                }
            } else if ("heavy_db".equals(type)) {
                if (SystemConfig.CACHE_ENABLED) {
                    operation = "Cached Aggregation";
                } else {
                    String sql = "SELECT status, AVG(amount) FROM orders GROUP BY status";
                    jdbcTemplate.queryForList(sql);
                }
            } else {
                Thread.sleep(random.nextInt(200));
            }
        } catch (Exception e) {
            return String.format("Error executing [%s]: %s", operation, e.getMessage());
        }

        long end = System.currentTimeMillis();
        return String.format("[%s] completed in %d ms", operation, (end - start));
    }

    // ==========================================
    //  NEW GRAPHQL MUTATION (Manual Trace)
    // ==========================================
    @MutationMapping
    public SimulationResponse submitTrace(@Argument String traceId,
                                          @Argument int duration,
                                          @Argument String query) {

        // 1. Trigger Governance (Create Proposal in DB)
        String suggestion = "CREATE INDEX idx_simulated ON orders(user_id)";
        governanceService.proposeOptimization(traceId, "simulation-service", query, suggestion);

        // 2. Trigger Dashboard Alert (WebSocket)
        alertPublisher.publish("simulation-service", duration, query, "WAITING FOR APPROVAL: " + suggestion);

        return new SimulationResponse("SUCCESS", "Trace simulated: " + traceId);
    }

    // Simple DTO for response
    public record SimulationResponse(String status, String message) {}
}