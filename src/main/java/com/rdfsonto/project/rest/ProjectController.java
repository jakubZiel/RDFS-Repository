package com.rdfsonto.project.rest;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.UNAUTHORIZED_RESOURCE_ACCESS;

import java.util.List;

import javax.annotation.security.RolesAllowed;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.infrastructure.security.service.AuthService;
import com.rdfsonto.project.database.ProjectNode;
import com.rdfsonto.project.service.ProjectService;
import com.rdfsonto.user.database.UserNode;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RolesAllowed("user")
@RequiredArgsConstructor
@RequestMapping("/neo4j/project")
public class ProjectController
{
    final private AuthService authService;
    final private ProjectService projectService;
    final private UserService userService;

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectNode> getProjectById(@PathVariable final long projectId)
    {
        authService.validateProjectAccess(projectId);

        final var project = projectService.findById(projectId);
        if (project.isEmpty())
        {
            log.info("Project id: {} does not exist", projectId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.of(project);
    }

    @GetMapping("/all/{username}")
    public ResponseEntity<List<ProjectNode>> getAllProjectsByUser(@PathVariable final String username)
    {
        final var user = userService.findByUsername(username);
        authService.validateUserAccess(user.map(UserNode::getId).orElse(null));

        if (userService.findByUsername(username).isEmpty())
        {
            log.info("User name: {} does not exist. Can not get all projects", username);
            return ResponseEntity.notFound().build();
        }
        final var projects = projectService.findProjectNodesByUsername(username);
        return ResponseEntity.ok(projects);
    }

    @GetMapping
    public ResponseEntity<List<ProjectNode>> getAllProjects()
    {
        return ResponseEntity.ok(projectService.findAll());
    }

    @PostMapping
    public ResponseEntity<ProjectNode> create(@RequestBody final CreateProjectRequest createProjectRequest)
    {
        authService.validateUserAccess(createProjectRequest.userId());

        final var projectName = createProjectRequest.projectName();
        final var userId = createProjectRequest.userId();

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

        final var saved = projectService.save(projectName, owner.get());

        return ResponseEntity.ok(saved);
    }

    @PutMapping
    public ResponseEntity<ProjectNode> update(final ProjectNode updatedProject)
    {
        authService.validateProjectAccess(updatedProject.getId());

        final var original = projectService.findById(updatedProject.getId());
        if (original.isEmpty())
        {
            log.info("Project id: {} does not exist, can not be updated", updatedProject.getId());
            return ResponseEntity.notFound().build();
        }

        final var afterUpdate = projectService.update(updatedProject);

        return ResponseEntity.ok(afterUpdate);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ProjectNode> delete(@PathVariable final long projectId)
    {
        authService.validateProjectAccess(projectId);

        final var project = projectService.findById(projectId);
        if (project.isEmpty())
        {
            log.info("Project id: {} can not be deleted, because it does not exist", projectId);
            return ResponseEntity.notFound().build();
        }

        projectService.delete(project.get());

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ClassNodeException.class)
    public ResponseEntity<?> handle(final ClassNodeException classNodeException)
    {
        if (classNodeException.getErrorCode() == UNAUTHORIZED_RESOURCE_ACCESS)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(classNodeException.getMessage());
        }
        return ResponseEntity.badRequest().body(classNodeException.getErrorCode());
    }
}