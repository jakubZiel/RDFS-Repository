package com.rdfsonto.rdfsonto.service.project;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rdfsonto.rdfsonto.repository.project.ProjectNode;
import com.rdfsonto.rdfsonto.repository.project.ProjectRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService
{
    private final ProjectRepository projectRepository;

    @Override
    public Optional<ProjectNode> findById(final long projectId)
    {
        return Optional.empty();
    }

    @Override
    public Optional<ProjectNode> findProjectByNameAndUserId(final String projectName, final long userId)
    {
        return projectRepository.findProjectByNameAndUserId(projectName, userId);
    }

    @Override
    public List<ProjectNode> findProjectNodesByUsername(final String username)
    {
        return projectRepository.findProjectNodesByUser(username);
    }

    @Override
    public List<ProjectNode> findAll()
    {
        return projectRepository.findAll();
    }

    @Override
    public ProjectNode save(final ProjectNode project)
    {
        return projectRepository.save(project);
    }

    @Override
    public void delete(final ProjectNode project)
    {
        if (projectRepository.existsById(project.getId()))
        {
            log.warn("Attempted to deleted non-existing project id: {}", project.getId());
            return;
        }

        projectRepository.delete(project);
    }
}
