package com.rdfsonto.rdfsonto.controller;

import com.rdfsonto.rdfsonto.model.ClassNode;
import com.rdfsonto.rdfsonto.repository.ClassNodeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/neo4j/class")
public class ClassController
{

    ClassNodeRepository repository;

    @Autowired
    public ClassController(ClassNodeRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    Optional<ClassNode> getClassNodeById(@PathVariable long id)
    {
        final var node = repository.findById(id);
        final var props = repository.getAllNodeProperties(id);

        node.ifPresent(nodeVal -> nodeVal.setProperties(props));
        return node;
    }

    @GetMapping("/count")
    long getClassNodesCount()
    {
        return repository.count();
    }

    @GetMapping("/all")
    Collection<ClassNode> getAllClassNodes()
    {
        return repository.findAll();
    }

    @GetMapping("/all/{user}/{project}")
    Collection<ClassNode> getAllClassNodesInProject(@PathVariable String user, @PathVariable String project)
    {
        return null;
    }

    @GetMapping("/{id}/props")
    Collection<String> getNodesProperties(@PathVariable long id)
    {
        return repository.getAllNodeProperties(id);
    }

    @PutMapping("/set_property/{id}/{propertyName}/{propertyValue}")
    ResponseEntity<?> setProperty(@PathVariable long id, @PathVariable String propertyName, @PathVariable String propertyValue)
    {

        return ResponseEntity.ok().build();
    }
}
