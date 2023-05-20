package com.rdfsonto.classnode.rest;

import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode;
import com.rdfsonto.classnode.service.ClassNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/class")
public class ClassNodeController
{
    private final ClassNodeService classNodeService;
    private final NodeChangeEventHandler nodeChangeEventHandler;

    @GetMapping("/{id}")
    ResponseEntity<?> getClassNodeById(@PathVariable final long id, @RequestParam final long projectId)
    {
        final var node = classNodeService.findById(projectId, id);
        return node.isPresent() ? ResponseEntity.of(node) : ResponseEntity.notFound().build();
    }

    @PostMapping("/ids")
    ResponseEntity<?> getClassNodesById(@RequestParam final long projectId, @RequestBody final List<Long> nodeIds)
    {
        if (nodeIds == null || nodeIds.isEmpty())
        {
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(classNodeService.findByIds(projectId, nodeIds));
    }

    @GetMapping("/neighbours/{id}")
    ResponseEntity<?> getClassNodeNeighbours(@PathVariable final long id, @RequestParam final int maxDistance, @RequestParam final long projectId)
    {
        final var neighbours = classNodeService.findNeighbours(projectId, id, maxDistance, List.of());
        return ResponseEntity.ok(neighbours);
    }

    @GetMapping("/neighbours/uri")
    ResponseEntity<?> getClassNodeNeighboursByUri(@RequestParam final String uri,
                                                  @RequestParam final int maxDistance,
                                                  @RequestParam final long projectId)
    {
        final var neighbours = classNodeService.findNeighboursByUri(projectId, uri, maxDistance, List.of());
        return ResponseEntity.ok(neighbours);
    }

    @PostMapping
    ResponseEntity<?> createNode(@RequestBody final ClassNode node, final long projectId)
    {
        return ResponseEntity.ok(classNodeService.save(projectId, node));
    }

    @PutMapping
    ResponseEntity<?> updateNode(@RequestBody final ClassNode nodeUpdate, final long projectId)
    {
        return ResponseEntity.ok(classNodeService.save(projectId, nodeUpdate));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteNode(@PathVariable final long id)
    {
        classNodeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/multiple")
    ResponseEntity<?> multipleNodeUpdates(@RequestBody List<NodeChangeEvent> events, @RequestParam final long projectId)
    {
        if (events == null || events.isEmpty())
        {
            return ResponseEntity.badRequest().body(ClassNodeExceptionErrorCode.EMPTY_REQUEST);
        }
        return ResponseEntity.ok(nodeChangeEventHandler.handleEvents(events, projectId));
    }

    @GetMapping("/metadata")
    ResponseEntity<?> getProjectNodeMetaData(@RequestParam final long projectId)
    {
        return ResponseEntity.ok(classNodeService.findProjectNodeMetaData(projectId));
    }

    @PostMapping("/filter")
    ResponseEntity<?> getNodesFiltered(@RequestBody final FilterPropertyRequest request)
    {
        return ResponseEntity.ok(
            classNodeService.findByPropertiesAndLabels(
                request.projectId(),
                request.labels(),
                request.filterConditions()));
    }

    @ExceptionHandler(ClassNodeException.class)
    public ResponseEntity<?> handle(final ClassNodeException classNodeException)
    {
        // TODO REMOVE
        classNodeException.printStackTrace();

        if (classNodeException.getErrorCode() == ClassNodeExceptionErrorCode.DATABASE_INTERNAL_ERROR)
        {
            return ResponseEntity.internalServerError().build();
        }

        log.warn(classNodeException.getMessage());
        return ResponseEntity.badRequest().body(classNodeException.getErrorCode());
    }
}