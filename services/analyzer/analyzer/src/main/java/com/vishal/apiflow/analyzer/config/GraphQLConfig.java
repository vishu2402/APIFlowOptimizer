package com.vishal.apiflow.analyzer.config;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQLConfig {

    @Bean
    public GraphQLSchema graphQLSchema() {
        String schema = """
            type Query {
                getSlowestQueries(limit: Int): [TraceRecord]
                getAnomalyCount: Int
            }
            type Subscription {
                liveAlerts: AnomalyAlert
            }
            type TraceRecord {
                traceId: String
                serviceName: String
                operationName: String
                durationMs: Float
                sqlText: String
            }
            type AnomalyAlert {
                serviceName: String
                latency: Float
                culpritSql: String
                aiSuggestion: String
                timestamp: String
            }
        """;

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }
}