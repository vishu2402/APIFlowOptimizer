package com.vishal.apiflow.ingestion.repository;

import com.vishal.apiflow.ingestion.model.RawEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RawEventRepository extends JpaRepository<RawEvent, UUID> {
}