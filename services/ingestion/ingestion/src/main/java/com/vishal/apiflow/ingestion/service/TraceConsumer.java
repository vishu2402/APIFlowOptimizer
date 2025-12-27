package com.vishal.apiflow.ingestion.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.vishal.apiflow.ingestion.model.RawEvent;
import com.vishal.apiflow.ingestion.model.Trace;
import com.vishal.apiflow.ingestion.repository.RawEventRepository;
import com.vishal.apiflow.ingestion.repository.TraceRepository;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.TracesData;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TraceConsumer {

    private final TraceRepository traceRepository;
    private final RawEventRepository rawEventRepository;

    public TraceConsumer(TraceRepository traceRepository, RawEventRepository rawEventRepository) {
        this.traceRepository = traceRepository;
        this.rawEventRepository = rawEventRepository;
    }

    @KafkaListener(topics = "apiflow-traces", groupId = "ingestion-group")
    @Transactional
    public void consumeTraces(byte[] message) {
        try {
            TracesData tracesData = TracesData.parseFrom(message);

            List<Trace> tracesToSave = new ArrayList<>();
            List<RawEvent> eventsToSave = new ArrayList<>();

            tracesData.getResourceSpansList().forEach(resourceSpans -> {

                String serviceName = resourceSpans.getResource().getAttributesList().stream()
                        .filter(attr -> attr.getKey().equals("service.name"))
                        .findFirst()
                        .map(attr -> attr.getValue().getStringValue())
                        .orElse("unknown");

                resourceSpans.getScopeSpansList().forEach(scopeSpans -> {
                    for (Span span : scopeSpans.getSpansList()) {

                        Trace trace = new Trace();
                        trace.setTraceId(bytesToHex(span.getTraceId().toByteArray()));
                        trace.setSpanId(bytesToHex(span.getSpanId().toByteArray()));

                        if (!span.getParentSpanId().isEmpty()) {
                            trace.setParentSpanId(bytesToHex(span.getParentSpanId().toByteArray()));
                        }

                        trace.setServiceName(serviceName);
                        trace.setOperationName(span.getName());

                        trace.setStartTime(nanoToDateTime(span.getStartTimeUnixNano()));
                        trace.setEndTime(nanoToDateTime(span.getEndTimeUnixNano()));

                        long durationNano = span.getEndTimeUnixNano() - span.getStartTimeUnixNano();
                        trace.setDurationMs(durationNano / 1_000_000.0);

                        trace.setStatusCode(span.getStatus().getCode().name());

                        tracesToSave.add(trace);

                        try {
                            RawEvent rawEvent = new RawEvent();
                            rawEvent.setTraceId(trace.getTraceId());
                            rawEvent.setSpanId(trace.getSpanId());

                            String jsonPayload = JsonFormat.printer().print(span);
                            rawEvent.setPayload(jsonPayload);

                            eventsToSave.add(rawEvent);
                        } catch (InvalidProtocolBufferException e) {
                            System.err.println("Error converting span to JSON: " + e.getMessage());
                        }
                    }
                });
            });

            if (!tracesToSave.isEmpty()) {
                traceRepository.saveAll(tracesToSave);
                rawEventRepository.saveAll(eventsToSave);
                System.out.println("Saved " + tracesToSave.size() + " spans to PostgreSQL.");
            }

        } catch (InvalidProtocolBufferException e) {
            System.err.println("Failed to parse Kafka message: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private LocalDateTime nanoToDateTime(long nano) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(nano / 1_000_000),
                ZoneId.systemDefault());
    }
}