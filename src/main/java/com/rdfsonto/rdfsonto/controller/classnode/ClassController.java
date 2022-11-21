package com.rdfsonto.rdfsonto.controller.classnode;

import java.util.Collection;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.repository.classnode.ClassNode;
import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/class")
public class ClassController
{
    private final ClassNodeRepository repository;

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
