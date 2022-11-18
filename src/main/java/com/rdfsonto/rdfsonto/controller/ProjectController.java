package com.rdfsonto.rdfsonto.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.model.ProjectNode;
import com.rdfsonto.rdfsonto.repository.ProjectRepository;
import com.rdfsonto.rdfsonto.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/project")
public class ProjectController
{
    final private ProjectRepository projectRepository;
    final private UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<ProjectNode> getProjectById(@PathVariable final long id)
    {
        final var project = projectRepository.findById(id);

        if (project.isEmpty())
        {
            log.info("Project id: {} does not exist.", id);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.of(project);
    }

    @GetMapping("/all/{user}")
    public ResponseEntity<List<ProjectNode>> getAllProjectsByUser(@PathVariable final String user)
    {
        if (userRepository.findByUsername(user).isEmpty())
        {
            log.info("User name: {} does not exist. Can not get all projects", user);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.of(projectRepository.findProjectNodesByUser(user));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProjectNode>> getAllProjects()
    {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<ProjectNode> create(final String projectName, final long userId)
    {
        final var owner = userRepository.findById(userId);

        if (owner.isEmpty())
        {
            log.info("Can not create project user id: {} does not exist", userId);
            return ResponseEntity.notFound().build();
        }

        if (projectRepository.findProjectByNameAndUser(projectName, userId) != null)
        {
            log.info("Project name: {} already exists for an user id: {}", projectName, userId);
            return ResponseEntity.badRequest().build();
        }

        final var project = ProjectNode.builder()
            .withOwner(owner.get())
            .withProjectName(projectName)
            .build();

        return ResponseEntity.ok(project);
    }

    @PutMapping
    public ResponseEntity<ProjectNode> update(final ProjectNode updatedProject)
    {
        final var original = projectRepository.findById(updatedProject.getId());

        if (original.isEmpty())
        {
            log.info("Project id: {} does not exist, can not be updated.", updatedProject.getId());
            return ResponseEntity.notFound().build();
        }

        final var afterUpdate = projectRepository.save(updatedProject);

        return ResponseEntity.ok(afterUpdate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProjectNode> delete(@PathVariable final long id)
    {
        final var project = projectRepository.findById(id);

        if (project.isEmpty())
        {
            log.info("Project id: {} can not be deleted, because it does not exist", id);
        }

        projectRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
