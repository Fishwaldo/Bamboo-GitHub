package com.carolynvs.gitallthethings.webhook;

import com.atlassian.bamboo.build.*;
import com.atlassian.bamboo.build.creation.*;
import com.atlassian.bamboo.plan.*;
import com.atlassian.bamboo.plan.branch.*;
import com.atlassian.bamboo.plan.cache.*;
import com.atlassian.bamboo.variable.*;
import com.atlassian.user.*;
import com.carolynvs.gitallthethings.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class PlanTrigger
{
    private final BranchDetectionService branchDetectionService;
    private final CachedPlanManager cachedPlanManager;
    private final PlanManager planManager;
    private final VariableConfigurationService variableConfigurationService;
    private final PlanExecutionManager planExecutionManager;
    private final BambooLinkBuilder bambooLinkBuilder;

    public PlanTrigger(BranchDetectionService branchDetectionService, CachedPlanManager cachedPlanManager,
                       PlanManager planManager, VariableConfigurationService variableConfigurationService,
                       PlanExecutionManager planExecutionManager, BambooLinkBuilder bambooLinkBuilder)
    {
        this.branchDetectionService = branchDetectionService;
        this.cachedPlanManager = cachedPlanManager;
        this.planManager = planManager;
        this.variableConfigurationService = variableConfigurationService;
        this.planExecutionManager = planExecutionManager;
        this.bambooLinkBuilder = bambooLinkBuilder;
    }

    public String execute(PlanKey planKey, User user, Map<String, String> variables)
    {
        ImmutableChain plan = cachedPlanManager.getMasterPlan(planKey);

        Map<String, String> params = new HashMap<String, String>();

        ExecutionRequestResult result = planExecutionManager.startManualExecution(plan, user, params, variables);
        return bambooLinkBuilder.getBuildUrl(result.getPlanResultKey().toString());
    }

    public void createPullRequestBranchPlan(PlanKey masterPlanKey, PullRequest pullRequest)
            throws PlanCreationDeniedException
    {
        ImmutableChain masterPlan = cachedPlanManager.getMasterPlan(masterPlanKey);

        ImmutableChainBranch existingPlan = findBranchPlan(masterPlan, pullRequest.Number);
        if(existingPlan != null)
            return;

        createBranchPlan(masterPlan, pullRequest);
    }

    private void createBranchPlan(ImmutableChain masterPlan, PullRequest pullRequest)
            throws PlanCreationDeniedException
    {
        String branchPlanName = String.format("Pull Request %s - %s", pullRequest.Number, pullRequest.Title);

        PlanKey branchPlanKey = branchDetectionService.createChainBranch(masterPlan, branchPlanName, null, null, PlanCreationService.EnablePlan.ENABLED, true);
        setPullRequestVariables(branchPlanKey, pullRequest);
    }

    private void setPullRequestVariables(PlanKey planKey, PullRequest pullRequest)
    {
        // Set the variables on the plan so that we can kick a build without custom variables
        Plan plan = planManager.getPlanByKey(planKey);

        PullRequestBuildContext pullRequestContext = new PullRequestBuildContext();
        Map<String, String> variables = pullRequestContext.createPullRequestVariables(pullRequest);
        for(Map.Entry<String, String> variable : variables.entrySet())
        {
            variableConfigurationService.createPlanVariable(plan, variable.getKey(), variable.getValue());
        }
    }

    @Nullable
    private ImmutableChainBranch findBranchPlan(ImmutableChain plan, Integer pullRequestNumber)
    {
        List<ImmutableChainBranch> branchPlans = cachedPlanManager.getBranchesForChain(plan);
        for(ImmutableChainBranch branchPlan : branchPlans)
        {
            List<VariableDefinition> planVariables = branchPlan.getVariables();
            for(VariableDefinition planVar : planVariables)
            {
                if(PullRequestBuildContext.PULLREQUEST_NUMBER_VAR.equals(planVar.getKey()) &&
                        pullRequestNumber.toString().equals(planVar.getValue()))
                {
                    return branchPlan;
                }
            }
        }

        return null;
    }
}