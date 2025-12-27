package com.vishal.apiflow.analyzer.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CostEstimator {

    private final JdbcTemplate jdbcTemplate;

    public CostEstimator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public double getQueryCost(String sql) {
        try {
            String executableSql = sql.replace("?", "'1'");
            String explainSql = "EXPLAIN (FORMAT JSON) " + executableSql;
            String jsonResult = jdbcTemplate.queryForObject(explainSql, String.class);

            Pattern pattern = Pattern.compile("\"Total Cost\":\\s*([0-9.]+)");
            Matcher matcher = pattern.matcher(jsonResult);

            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
        }
        return 0.0;
    }
}