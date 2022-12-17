package com.rdfsonto.rdfsonto.controller.classnode;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeRepository;
import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeVo;
import com.rdfsonto.rdfsonto.service.classnode.ClassNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/class")
public class ClassNodeController
{
    private final ClassNodeRepository repository;
    private final ClassNodeService classNodeService;

    @GetMapping("/{id}")
    ResponseEntity<?> getClassNodeById(@PathVariable final long id)
    {
        final var node = classNodeService.getClassNodeById(id);

        if (node.isEmpty())
        {
            log.info("Node id: {} does not exist", id);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(node.get());
    }

    @PostMapping("/ids")
    ResponseEntity<?> getClassNodesById(@RequestBody final List<Long> nodeIds)
    {
        if (nodeIds.isEmpty())
        {
            return ResponseEntity.badRequest().body("invalid_body_empty_ids");
        }

        final var nodes = classNodeService.getClassNodesByIds(nodeIds);

        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/count")
    long getClassNodesCount()
    {
        return repository.count();
    }

    @GetMapping("/all")
    Collection<ClassNodeVo> getAllClassNodes()
    {
        return repository.findAll();
    }

    @GetMapping("/all/{user}/{project}")
    Collection<ClassNodeVo> getAllClassNodesInProject(@PathVariable String user, @PathVariable String project)
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
