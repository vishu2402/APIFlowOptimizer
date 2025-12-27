package com.vishal.apiflow.analyzer.repository;

import com.vishal.apiflow.analyzer.model.OptimizationProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProposalRepository extends JpaRepository<OptimizationProposal, Long> {
    List<OptimizationProposal> findByStatusOrderByCreatedAtDesc(String status);
}