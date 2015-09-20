package com.carolynvs.gitallthethings.webhook;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.bamboo.admin.configuration.AdministrationConfigurationService;
import com.atlassian.bamboo.build.*;
import com.atlassian.bamboo.build.creation.*;
import com.atlassian.bamboo.plan.*;
import com.atlassian.bamboo.plan.branch.*;
import com.atlassian.bamboo.plan.cache.*;
import com.atlassian.bamboo.variable.*;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@AnonymousAllowed
@Path("/pullrequest-trigger")
@Consumes({MediaType.APPLICATION_JSON})
public class PullRequestTriggerResource
{
    private final PullRequestBuilder pullRequestBuilder;
    private final GitHubCommunicator github;
    private final PluginDataManager pluginData;
    private final PlanTrigger planTrigger;

    public PullRequestTriggerResource(BranchDetectionService branchDetectionService, CachedPlanManager cachedPlanManager, PlanManager planManager,
                                      VariableConfigurationService variableConfigurationService,
                                      PlanExecutionManager planExecutionManager, AdministrationConfigurationService administrationConfigurationService,
                                      ActiveObjects ao)
    {
        this.github = new GitHubCommunicator();
        this.pluginData = new PluginDataManager(ao);
        BambooLinkBuilder bambooLinkBuilder = new BambooLinkBuilder(administrationConfigurationService);
        this.pullRequestBuilder = new PullRequestBuilder(branchDetectionService, cachedPlanManager, planManager, variableConfigurationService, planExecutionManager, pluginData, github, bambooLinkBuilder);

        this.planTrigger = new PlanTrigger(branchDetectionService, cachedPlanManager, planManager, variableConfigurationService, planExecutionManager, bambooLinkBuilder);
    }

    @POST
    @Path("test/{number}/{title}")
    public Response test(@PathParam("number") Integer pullRequestNumber, @PathParam("title") String pullRequestTitle)
    {
        String planKey = "TEST-TEST";
        try {
            PullRequest pullRequest = new PullRequest();
            pullRequest.Number = pullRequestNumber;
            pullRequest.Title = pullRequestTitle;
            planTrigger.createPullRequestBranchPlan(PlanKeys.getPlanKey(planKey), pullRequest);
        } catch (PlanCreationDeniedException ex) {
            Response.serverError().entity(new ServerError(ex).toJson()).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("{plan-key}")
    public Response post(@PathParam("plan-key") String planKey, @HeaderParam("X-GitHub-Event") String event, @HeaderParam("X-Hub-Signature") String signature, String jsonBody)
    {
        if(isPing(event))
            return Response.ok().build();

        PullRequestEvent pullRequestEvent = parsePullRequestEvent(jsonBody);
        if(pullRequestEvent == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        String webHookSecret = pluginData.getConfig(planKey).getSecret();
        if(!github.validWebHook(webHookSecret, jsonBody, signature))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        if(!isPullRequestContentChanged(pullRequestEvent))
            return Response.status(Response.Status.ACCEPTED).build();

        try {
            planTrigger.createPullRequestBranchPlan(PlanKeys.getPlanKey(planKey), pullRequestEvent.PullRequest);
            pullRequestBuilder.build(planKey, pullRequestEvent);
        } catch (Exception ex) {
            return Response.serverError().entity(new ServerError(ex).toJson()).build();
        }

        return Response.status(Response.Status.OK).build();
    }

    private boolean isPing(String event)
    {
        return event.equals("ping");
    }

    private boolean isPullRequestContentChanged(PullRequestEvent pullRequestEvent)
    {
        return PullRequestAction.OPENED.equals(pullRequestEvent.Action) || PullRequestAction.SYNCHRONIZE.equals(pullRequestEvent.Action);
    }

    private PullRequestEvent parsePullRequestEvent(String jsonBody)
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonBody, PullRequestEvent.class);
        }
        catch (IOException e) {
            return null;
        }
    }
}