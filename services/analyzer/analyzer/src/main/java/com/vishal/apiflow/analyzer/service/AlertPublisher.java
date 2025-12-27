package com.vishal.apiflow.analyzer.service;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class AlertPublisher {

    private final Sinks.Many<AnomalyAlert> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer(100, false);

    public void publish(String service, double latency, String sql, String suggestion) {
        // Format time for easy reading on the Dashboard
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        AnomalyAlert alert = new AnomalyAlert(
                service,
                latency,
                sql,
                suggestion,
                timestamp
        );

        Sinks.EmitResult result = sink.tryEmitNext(alert);

        if (result.isSuccess()) {
            System.out.println("📡 Broadcast: Sent alert for " + service + " to UI.");
        }
    }

    public Flux<AnomalyAlert> getAlertStream() {
        return sink.asFlux();
    }

    public record AnomalyAlert(
            String serviceName,
            double latency,
            String culpritSql,
            String aiSuggestion,
            String timestamp
    ) {}
}