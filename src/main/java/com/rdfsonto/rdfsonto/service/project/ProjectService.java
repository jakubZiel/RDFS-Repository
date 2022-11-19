package com.rdfsonto.rdfsonto.service.project;

import java.util.List;
import java.util.Optional;

import com.rdfsonto.rdfsonto.repository.project.ProjectNode;


public interface ProjectService
{
    Optional<ProjectNode> findById(long projectId);

    Optional<ProjectNode> findProjectByNameAndUserId(String projectName, long userId);

    List<ProjectNode> findProjectNodesByUsername(String username);

    List<ProjectNode> findAll();

    ProjectNode save(ProjectNode project);

    void delete(ProjectNode project);
}
