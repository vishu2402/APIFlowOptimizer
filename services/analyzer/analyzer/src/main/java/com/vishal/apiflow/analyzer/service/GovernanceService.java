package com.vishal.apiflow.analyzer.service;

import com.vishal.apiflow.analyzer.model.OptimizationProposal;
import com.vishal.apiflow.analyzer.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GovernanceService {

    private final ProposalRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${governance.auto-apply:false}")
    private boolean autoApply;

    public GovernanceService(ProposalRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public OptimizationProposal proposeOptimization(
            String traceId,
            String service,
            String sql,
            String suggestion) {

        OptimizationProposal proposal =
                new OptimizationProposal(traceId, service, sql, suggestion);

        if (autoApply && isSafeSql(suggestion)) {
            proposal.setStatus("APPLIED");
            proposal.setReviewedAt(LocalDateTime.now());
            executeFix(suggestion);
        }

        return repository.save(proposal);
    }

    public List<OptimizationProposal> getPendingProposals() {
        return repository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Transactional
    public void approveProposal(Long id) {
        repository.findById(id).ifPresent(p -> {

            if (!"PENDING".equals(p.getStatus())) return;

            p.setStatus("APPLIED");
            p.setReviewedAt(LocalDateTime.now());
            repository.save(p);

            if (isSafeSql(p.getAiSuggestion())) {
                executeFix(p.getAiSuggestion());
            }
        });
    }


    public void rejectProposal(Long id) {
        repository.findById(id).ifPresent(p -> {
            p.setStatus("REJECTED");
            p.setReviewedAt(LocalDateTime.now());
            repository.save(p);
        });
    }

    private boolean isSafeSql(String sql) {
        if (sql == null) return false;
        String s = sql.toUpperCase();
        return s.startsWith("ANALYZE") || s.startsWith("CREATE INDEX");
    }

    private void executeFix(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            System.err.println("SQL execution failed: " + e.getMessage());
        }
    }

    public OptimizationProposal getById(Long id) {
        return repository.findById(id).orElse(null);
    }
}
