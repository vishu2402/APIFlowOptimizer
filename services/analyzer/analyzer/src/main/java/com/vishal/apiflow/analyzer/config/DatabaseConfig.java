package com.vishal.apiflow.analyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    // 1. Manually define the Data Source (The Connection)
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        // We hardcode this HERE to bypass any property file issues for now.
        // Once this works, we can switch back to variables.
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5435/apiflow");
        dataSource.setUsername("admin");
        dataSource.setPassword("root");
        return dataSource;
    }

    // 2. Manually define the JdbcTemplate (The Tool you need)
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}