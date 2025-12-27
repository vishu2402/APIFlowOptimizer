package com.vishal.apiflow.analyzer.service;

import org.springframework.stereotype.Service;

@Service
public class LlmClient {
    public OptimizationResult rewrite(String badSql) {
        String improvedSql = "";
        String explanation = "";

        if (badSql.contains("mock_orders") && badSql.contains("user_id")) {
            improvedSql = "CREATE INDEX idx_mock_orders_user_id ON mock_orders(user_id);";
            explanation = "The query is performing a Sequential Scan on 'mock_orders'. Adding a B-Tree index will optimize this.";
        }
        else if (badSql.contains("JOIN") && badSql.contains("mock_users")) {
            improvedSql = "CREATE INDEX idx_mock_users_region ON mock_users(region);";
            explanation = "The JOIN operation is slow. Indexing the grouping column 'region' will optimize the sort.";
        }
        else {
            improvedSql = "VACUUM ANALYZE mock_orders;";
            explanation = "General maintenance.";
        }

        return new OptimizationResult(improvedSql, explanation);
    }

    public static class OptimizationResult {
        public String sql;
        public String reasoning;

        public OptimizationResult(String sql, String reasoning) {
            this.sql = sql;
            this.reasoning = reasoning;
        }
    }
}