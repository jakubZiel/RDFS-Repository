package com.rdfsonto.rdfsonto.controller.project;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.repository.project.ProjectNode;
import com.rdfsonto.rdfsonto.service.project.ProjectService;
import com.rdfsonto.rdfsonto.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/project")
public class ProjectController
{
    final private ProjectService projectService;
    final private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ProjectNode> getProjectById(@PathVariable final long id)
    {
        final var project = projectService.findById(id);

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
        if (userService.findByUsername(user).isEmpty())
        {
            log.info("User name: {} does not exist. Can not get all projects", user);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(projectService.findProjectNodesByUsername(user));
    }

    @GetMapping
    public ResponseEntity<List<ProjectNode>> getAllProjects()
    {
        return ResponseEntity.ok(projectService.findAll());
    }

    @PostMapping
    public ResponseEntity<ProjectNode> create(final String projectName, final long userId)
    {
        final var owner = userService.findById(userId);

        if (owner.isEmpty())
        {
            log.info("Can not create project user id: {} does not exist", userId);
            return ResponseEntity.notFound().build();
        }

        if (projectService.findProjectByNameAndUserId(projectName, userId).isPresent())
        {
            log.info("Project name: {} already exists for an user id: {}", projectName, userId);
            return ResponseEntity.badRequest().build();
        }

        final var project = ProjectNode.builder()
            .withOwner(owner.get())
            .withProjectName(projectName)
            .build();

        final var saved = projectService.save(project);

        return ResponseEntity.ok(saved);
    }

    @PutMapping
    public ResponseEntity<ProjectNode> update(final ProjectNode updatedProject)
    {
        final var original = projectService.findById(updatedProject.getId());

        if (original.isEmpty())
        {
            log.info("Project id: {} does not exist, can not be updated.", updatedProject.getId());
            return ResponseEntity.notFound().build();
        }

        final var afterUpdate = projectService.save(updatedProject);

        return ResponseEntity.ok(afterUpdate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProjectNode> delete(@PathVariable final long id)
    {
        final var project = projectService.findById(id);

        if (project.isEmpty())
        {
            log.info("Project id: {} can not be deleted, because it does not exist", id);
            return ResponseEntity.notFound().build();
        }

        projectService.delete(project.get());

        return ResponseEntity.noContent().build();
    }
}
