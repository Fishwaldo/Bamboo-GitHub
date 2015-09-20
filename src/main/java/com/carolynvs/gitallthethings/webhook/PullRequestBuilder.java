package com.carolynvs.gitallthethings.webhook;

import com.atlassian.bamboo.plan.*;
import com.atlassian.bamboo.plan.branch.*;
import com.atlassian.bamboo.plan.cache.*;
import com.atlassian.bamboo.variable.*;
import com.atlassian.user.*;
import com.carolynvs.gitallthethings.*;

import java.util.*;

public class PullRequestBuilder
{
    private final PlanTrigger planTrigger;
    private final PluginDataManager pluginData;
    private final GitHubCommunicator github;

    public PullRequestBuilder(BranchDetectionService branchDetectionService, CachedPlanManager cachedPlanManager, PlanManager planManager,
                              VariableConfigurationService variableConfigurationService,
                              PlanExecutionManager planExecutionManager, PluginDataManager pluginData,
                              GitHubCommunicator github, BambooLinkBuilder bambooLinkBuilder)
    {
        this.pluginData = pluginData;
        this.github = github;
        this.planTrigger = new PlanTrigger(branchDetectionService, cachedPlanManager, planManager, variableConfigurationService, planExecutionManager, bambooLinkBuilder);
    }

    public void build(String planKey, PullRequestEvent pullRequestEvent)
            throws Exception
    {
        PullRequestBuildContext buildContext = new PullRequestBuildContext();
        Map<String, String> variables = buildContext.createPullRequestVariables(pullRequestEvent.PullRequest);

        User triggerUser = pluginData.getAssociatedUser(planKey, pullRequestEvent);
        String buildResultUrl = planTrigger.execute(PlanKeys.getPlanKey(planKey), triggerUser, variables);

        String token = pluginData.getConfig(planKey).getToken();
        GitHubSetCommitStatusRequest statusRequest = new GitHubSetCommitStatusRequest(GitHubCommitState.Pending, "The build is running", buildResultUrl);

        github.setPullRequestStatus(token, pullRequestEvent.PullRequest, statusRequest);
    }
}