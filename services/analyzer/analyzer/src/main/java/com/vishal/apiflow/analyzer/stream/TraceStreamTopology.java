package com.vishal.apiflow.analyzer.stream;

import com.vishal.apiflow.analyzer.model.EnrichedSpan;
import com.vishal.apiflow.analyzer.model.MetricAggregator;
import com.vishal.apiflow.analyzer.model.ServiceMetric;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.TracesData;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableKafkaStreams
public class TraceStreamTopology {

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        JsonSerde<EnrichedSpan> spanSerde = new JsonSerde<>(EnrichedSpan.class);
        JsonSerde<MetricAggregator> aggSerde = new JsonSerde<>(MetricAggregator.class);
        JsonSerde<ServiceMetric> metricSerde = new JsonSerde<>(ServiceMetric.class);

        KStream<String, byte[]> rawStream = streamsBuilder.stream(
                "apiflow-traces",
                Consumed.with(Serdes.String(), Serdes.ByteArray())
        );

        KStream<String, EnrichedSpan> enrichedStream = rawStream.flatMapValues(value -> {
            List<EnrichedSpan> list = new ArrayList<>();
            try {
                TracesData data = TracesData.parseFrom(value);
                data.getResourceSpansList().forEach(rs -> {
                    String service = rs.getResource().getAttributesList().stream()
                            .filter(a -> a.getKey().equals("service.name"))
                            .findFirst()
                            .map(a -> a.getValue().getStringValue())
                            .orElse("unknown");

                    rs.getScopeSpansList().forEach(ss -> {
                        for (Span span : ss.getSpansList()) {
                            double ms =
                                    (span.getEndTimeUnixNano()
                                            - span.getStartTimeUnixNano()) / 1_000_000.0;

                            boolean isErr =
                                    span.getStatus().getCode().name()
                                            .equals("STATUS_CODE_ERROR");

                            list.add(new EnrichedSpan(
                                    null,
                                    service,
                                    span.getName(),
                                    ms,
                                    null,
                                    isErr
                            ));
                        }
                    });
                });
            } catch (Exception ignored) {}
            return list;
        });

        KTable<Windowed<String>, MetricAggregator> windowedStats =
                enrichedStream
                        .groupBy(
                                (key, span) -> span.getServiceName(),
                                Grouped.with(Serdes.String(), spanSerde)
                        )
                        .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                        .aggregate(
                                MetricAggregator::new,
                                (key, span, agg) -> agg.add(span),
                                Materialized.with(Serdes.String(), aggSerde)
                        )
                        .suppress(
                                Suppressed.untilWindowCloses(
                                        Suppressed.BufferConfig.unbounded()
                                )
                        );

        windowedStats
                .toStream()
                .map((windowedKey, agg) -> {
                    double avg =
                            agg.count > 0 ? agg.totalDuration / agg.count : 0.0;

                    ServiceMetric metric = new ServiceMetric(
                            windowedKey.key(),
                            windowedKey.window().start(),
                            avg,
                            agg.errorCount
                    );

                    System.out.println("REAL-TIME WINDOW METRIC: " + metric);

                    return KeyValue.pair(windowedKey.key(), metric);
                })
                .to(
                        "ml-features",
                        Produced.with(Serdes.String(), metricSerde)
                );
    }
}
