package com.rdfsonto.rdfsonto.controller;

import com.rdfsonto.rdfsonto.model.ProjectNode;
import com.rdfsonto.rdfsonto.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/neo4j/project")
public class ProjectController {

    ProjectRepository repository;

    @Autowired
    public ProjectController(ProjectRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    Optional<ProjectNode> getProjectById(@PathVariable long id) {
        return repository.findById(id);
    }

    @GetMapping("/all/by_user/{user}")
    Collection<ProjectNode> getAllProjectsByUser(@PathVariable String user){
        return repository.getProjectNodeByUser(user);
    }

    @PostMapping("/add/empty")
    ResponseEntity<Boolean> createEmptyProject(String projectName, long userId) {
        if (repository.findProjectByNameAndUser(projectName, userId) != null)
            return ResponseEntity.badRequest().build();


        return ResponseEntity.ok(true);
    }
}
