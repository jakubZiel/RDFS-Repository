package com.rdfsonto.classnode.rest;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INTERNAL_ERROR;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.NEIGHBOURHOOD_TOO_BIG;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.UNAUTHORIZED_RESOURCE_ACCESS;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.classnode.database.RelationshipDirection;
import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode;
import com.rdfsonto.classnode.service.ClassNodeService;
import com.rdfsonto.infrastructure.security.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/class")
public class ClassNodeController
{
    private final AuthService authService;
    private final ClassNodeService classNodeService;
    private final NodeChangeEventHandler nodeChangeEventHandler;
    private final ClassNodeRestMapper classNodeRestMapper;

    @GetMapping("/{nodeId}")
    ResponseEntity<?> getClassNodeById(@PathVariable final long nodeId, @RequestParam final long projectId)
    {
        authService.validateNodeAccess(List.of(nodeId));

        final var node = classNodeService.findById(projectId, nodeId);
        return node.isPresent() ? ResponseEntity.of(node) : ResponseEntity.notFound().build();
    }

    @PostMapping("/ids")
    ResponseEntity<?> getClassNodesById(@RequestParam final long projectId, @RequestBody final List<Long> nodeIds)
    {
        authService.validateNodeAccess(nodeIds);

        if (nodeIds == null || nodeIds.isEmpty())
        {
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(classNodeService.findByIds(projectId, nodeIds));
    }

    @GetMapping("/neighbours/{nodeId}")
    ResponseEntity<?> getClassNodeNeighbours(@PathVariable final long nodeId,
                                             @RequestParam final int maxDistance,
                                             @RequestParam final long projectId,
                                             @RequestParam final Optional<RelationshipDirection> relationshipDirection)
    {
        authService.validateNodeAccess(List.of(nodeId));

        final var direction = relationshipDirection.orElse(RelationshipDirection.ANY);

        final var neighbours = classNodeService.findNeighbours(projectId, nodeId, maxDistance, List.of(), direction);
        return ResponseEntity.ok(neighbours);
    }

    @GetMapping("/neighbours/uri")
    ResponseEntity<?> getClassNodeNeighboursByUri(@RequestParam final String uri,
                                                  @RequestParam final int maxDistance,
                                                  @RequestParam final long projectId,
                                                  @RequestParam final Optional<RelationshipDirection> relationshipDirection)
    {
        final var direction = relationshipDirection.orElse(RelationshipDirection.ANY);

        final var neighbours = classNodeService.findNeighboursByUri(projectId, uri, maxDistance, List.of(), direction);
        return ResponseEntity.ok(neighbours);
    }

    @PostMapping
    ResponseEntity<?> createNode(@RequestBody final ClassNode node, final long projectId)
    {
        authService.validateProjectAccess(projectId);

        final var parsed = classNodeRestMapper.mapTypes(node);
        return ResponseEntity.ok(classNodeService.save(projectId, parsed));
    }

    @PutMapping
    ResponseEntity<?> updateNode(@RequestBody final ClassNode nodeUpdate, final long projectId)
    {
        authService.validateNodeAccess(List.of(nodeUpdate.id()));

        final var parsed = classNodeRestMapper.mapTypes(nodeUpdate);
        return ResponseEntity.ok(classNodeService.save(projectId, parsed));
    }

    @DeleteMapping("/{projectId}/{nodeId}")
    ResponseEntity<?> deleteNode(@PathVariable final long projectId, @PathVariable final long nodeId)
    {
        authService.validateNodeAccess(List.of(nodeId));

        classNodeService.deleteById(projectId, nodeId);
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
        authService.validateProjectAccess(projectId);

        return ResponseEntity.ok(classNodeService.findProjectNodeMetaData(projectId));
    }

    @PostMapping("/filter")
    ResponseEntity<?> getNodesFiltered(@RequestBody final FilterPropertyRequest request, final Pageable pageable)
    {
        authService.validateProjectAccess(request.projectId());

        final var result = classNodeService.findByPropertiesAndLabels(
            request.projectId(),
            request.labels(),
            request.filterConditions(),
            request.patterns(),
            pageable,
            request.searchAfter());

        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(ClassNodeException.class)
    public ResponseEntity<?> handle(final ClassNodeException classNodeException)
    {
        if (classNodeException.getErrorCode() == INTERNAL_ERROR)
        {
            return ResponseEntity.internalServerError().build();
        }

        if (classNodeException.getErrorCode() == UNAUTHORIZED_RESOURCE_ACCESS)
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(classNodeException.getMessage());
        }

        if (classNodeException.getErrorCode() == NEIGHBOURHOOD_TOO_BIG)
        {
            return ResponseEntity.badRequest().body(List.of(NEIGHBOURHOOD_TOO_BIG, classNodeException.getValue()));
        }

        log.warn(classNodeException.getMessage());
        return ResponseEntity.badRequest().body(classNodeException.getErrorCode());
    }
}