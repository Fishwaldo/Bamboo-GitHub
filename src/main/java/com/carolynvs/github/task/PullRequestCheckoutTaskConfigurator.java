package com.carolynvs.github.task;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plugins.git.GitCapabilityTypeModule;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskRequirementSupport;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class PullRequestCheckoutTaskConfigurator extends AbstractTaskConfigurator implements TaskRequirementSupport
{
    public static final String REPO_PATH = "repoPath";

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(ActionParametersMap params, TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        config.put(REPO_PATH, params.getString(REPO_PATH));

        return config;
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        context.put(REPO_PATH, taskDefinition.getConfiguration().get(REPO_PATH));
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        context.put(REPO_PATH, taskDefinition.getConfiguration().get(REPO_PATH));
    }

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition)
    {
        Set<Requirement> requirements = Sets.newHashSet();
        requirements.add(new RequirementImpl(GitCapabilityTypeModule.GIT_CAPABILITY, true, ".*"));
        return requirements;
    }
}