package com.vishal.apiflow.analyzer.stream;

import com.vishal.apiflow.analyzer.model.ServiceMetric;
import com.vishal.apiflow.analyzer.service.AlertPublisher;
import com.vishal.apiflow.analyzer.service.GovernanceService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AnomalyStreamProcessor {

    private final AlertPublisher alertPublisher;
    private final GovernanceService governanceService;

    private final Map<String, Boolean> activeIncidents = new ConcurrentHashMap<>();

    private static final long COOLDOWN_WINDOW_MS = 5 * 60_000;

    public AnomalyStreamProcessor(
            AlertPublisher alertPublisher,
            GovernanceService governanceService) {
        this.alertPublisher = alertPublisher;
        this.governanceService = governanceService;
    }

    @Bean
    public KStream<String, ServiceMetric> process(StreamsBuilder builder) {

        JsonSerde<ServiceMetric> metricSerde =
                new JsonSerde<>(ServiceMetric.class);

        KStream<String, ServiceMetric> stream =
                builder.stream(
                        "ml-features",
                        Consumed.with(Serdes.String(), metricSerde)
                );

        stream.foreach((key, metric) -> {

            String serviceName = metric.getServiceName();
            double avgLatency = metric.getAvgLatency();
            long errorCount = metric.getErrorCount();
            long windowStart = metric.getWindowStart();

            boolean isAnomalous = avgLatency > 200 || errorCount > 5;

            long cooldownBucket = windowStart / COOLDOWN_WINDOW_MS;
            String incidentKey = serviceName + "_" + cooldownBucket;

            boolean incidentActive =
                    activeIncidents.getOrDefault(incidentKey, false);

            if (isAnomalous && !incidentActive) {

                String incidentId = "INCIDENT-" + serviceName + "-" + windowStart;
                String suggestion = generateSmartFix(avgLatency, errorCount);

                governanceService.proposeOptimization(
                        incidentId,
                        serviceName,
                        "Aggregated (1-minute window)",
                        suggestion
                );

                alertPublisher.publish(
                        serviceName,
                        avgLatency,
                        "Windowed anomaly detected",
                        suggestion
                );

                activeIncidents.put(incidentKey, true);

                System.out.println(
                        "\n[KAFKA-ALERT] INCIDENT OPENED → " +
                                serviceName +
                                " | avg=" + avgLatency + "ms | errors=" + errorCount
                );
            }

            else if (!isAnomalous && incidentActive) {

                alertPublisher.publish(
                        serviceName,
                        avgLatency,
                        "Metrics normalized",
                        "System recovered automatically"
                );

                activeIncidents.remove(incidentKey);

                System.out.println(
                        "[KAFKA-ALERT] INCIDENT RESOLVED → " + serviceName
                );
            }
        });

        return stream;
    }

    private String generateSmartFix(double avgLatency, long errorCount) {

        if (errorCount > 5) {
            return "High error rate detected – inspect downstream dependencies";
        }

        if (avgLatency > 500) {
            return "Severe latency – consider DB indexing or caching";
        }

        return "Latency degradation – review recent deployments";
    }
}
