package com.vishal.apiflow.analyzer;

import com.vishal.apiflow.analyzer.model.OptimizationProposal;
import com.vishal.apiflow.analyzer.service.GovernanceService;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GovernanceResolver {

    private final GovernanceService governanceService;

    public GovernanceResolver(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @QueryMapping
    public List<OptimizationProposal> getPendingProposals() {
        System.out.println("getPendingProposals resolver CALLED");
        return governanceService.getPendingProposals();
    }

    @MutationMapping
    public OptimizationProposal approveProposal(@Argument Long id) {
        governanceService.approveProposal(id);
        return governanceService.getById(id); // direct DB fetch
    }

    @MutationMapping
    public OptimizationProposal rejectProposal(@Argument Long id) {
        governanceService.rejectProposal(id);
        return null;
    }
}
