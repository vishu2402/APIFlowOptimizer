package com.vishal.apiflow.ingestion.repository;

import com.vishal.apiflow.ingestion.model.Trace;
import com.vishal.apiflow.ingestion.model.TraceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceRepository extends JpaRepository<Trace, TraceId> {
}