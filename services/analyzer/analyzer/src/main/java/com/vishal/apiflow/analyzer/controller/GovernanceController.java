package com.vishal.apiflow.analyzer.controller;

import com.vishal.apiflow.analyzer.model.OptimizationProposal;
import com.vishal.apiflow.analyzer.service.GovernanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/governance")
@CrossOrigin(origins = "*")
public class GovernanceController {

    private final GovernanceService service;

    public GovernanceController(GovernanceService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<OptimizationProposal> pending() {
        return service.getPendingProposals();
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) {
        service.approveProposal(id);
    }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id) {
        service.rejectProposal(id);
    }
}
