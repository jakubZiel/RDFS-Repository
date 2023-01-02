package com.rdfsonto.rdfsonto.service.project;

import java.util.List;
import java.util.Optional;

import com.rdfsonto.rdfsonto.repository.project.ProjectNode;
import com.rdfsonto.rdfsonto.repository.user.UserNode;


public interface ProjectService
{
    Optional<ProjectNode> findById(long projectId);

    Optional<ProjectNode> findProjectByNameAndUserId(String projectName, long userId);

    List<ProjectNode> findProjectNodesByUsername(String username);

    List<ProjectNode> findAll();

    ProjectNode save(String projectName, UserNode user);

    ProjectNode update(ProjectNode update);

    void delete(ProjectNode project);

    String getProjectTag(ProjectNode project);
}
