package com.rdfsonto.rdfsonto.controller.classnode;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeRepository;
import com.rdfsonto.rdfsonto.service.classnode.ClassNode;
import com.rdfsonto.rdfsonto.service.classnode.ClassNodeService;
import com.rdfsonto.rdfsonto.service.project.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/class")
public class ClassNodeController
{
    private final ProjectService projectService;
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeService classNodeService;
    private final NodeChangeEventHandler nodeChangeEventHandler;

    @GetMapping("/{id}")
    ResponseEntity<?> getClassNodeById(@PathVariable final long id)
    {
        final var node = classNodeService.findById(id);

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

        final var nodes = classNodeService.findByIds(nodeIds);

        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/neighbours/{id}")
    ResponseEntity<?> getClassNodeNeighbours(@PathVariable final long id,
                                             final int maxDistance,
                                             @RequestParam final List<String> allowedRelationships)
    {
        if (!classNodeRepository.existsById(id))
        {
            return ResponseEntity.notFound().build();
        }

        final var neighbours = classNodeService.findNeighbours(id, maxDistance, allowedRelationships);

        return ResponseEntity.ok(neighbours);
    }

    @PostMapping
    ResponseEntity<?> createNode(@RequestBody final ClassNode node, final long projectId)
    {
        final var project = projectService.findById(projectId);

        if (project.isEmpty())
        {
            log.info("Project id: {} does not exist", projectId);
            return ResponseEntity.badRequest().body("invalid_project");
        }

        final var savedNode = classNodeService.save(node);

        if (savedNode.isEmpty())
        {
            log.info("Failed to save node : {}", node);
            return ResponseEntity.internalServerError().body("failed_node_save");
        }

        return ResponseEntity.ok(savedNode.get());
    }

    @PutMapping
    ResponseEntity<?> updateNode(@RequestBody final ClassNode nodeUpdate)
    {
        final var originalNode = classNodeService.findById(nodeUpdate.id());

        if (originalNode.isEmpty())
        {
            log.info("Node of id ");
            return ResponseEntity.notFound().build();
        }

        final var updatedNode = classNodeService.update(nodeUpdate);

        if (updatedNode.isEmpty())
        {
            log.info("Failed to update node:  {}", nodeUpdate);
            return ResponseEntity.internalServerError().body("failed_node_update");
        }

        return ResponseEntity.ok(updatedNode);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteNode(@PathVariable final long id)
    {
        if (!classNodeRepository.existsById(id))
        {
            log.info("Class node id: {} can not be deleted, because it does not exist", id);
            return ResponseEntity.notFound().build();
        }

        final var deleted = classNodeService.deleteById(id);

        if (deleted)
        {
            return ResponseEntity.noContent().build();
        }
        else
        {
            log.warn("Failed to delete node id {}, node has been already deleted", id);
            return ResponseEntity.internalServerError().body("error_delete_non_existing_node");
        }
    }

    @PutMapping("/multiple")
    ResponseEntity<?> multipleNodeUpdates(@RequestBody List<NodeChangeEvent> events)
    {
        if (events == null)
        {
            return ResponseEntity.badRequest().body("null_events");
        }

        final var responses = nodeChangeEventHandler.handleEvents(events);

        final var failedRequests = responses.stream()
            .filter(NodeChangeEventResponse::failed)
            .toList();

        if (failedRequests.isEmpty())
        {
            return ResponseEntity.ok(responses);
        }

        log.error("Failed requests: {}", failedRequests);
        return ResponseEntity.internalServerError().body(responses);
    }

    @GetMapping
    ResponseEntity<?> getProjectNodeMetaData(final long projectId)
    {
        final var project = projectService.findById(projectId);

        if (project.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }

        final var projectTag = projectService.getProjectTag(project.get());

        return ResponseEntity.ok(classNodeService.findProjectNodeMetaData(projectTag));
    }

    @GetMapping("/by_property/project/{projectId}}")
    ResponseEntity<?> getNodesByPropertyValue(@PathVariable final long projectId,
                                              @RequestParam final String propertyKey,
                                              @RequestParam final String propertyValue)
    {
        final var project = projectService.findById(projectId);

        if (project.isEmpty())
        {
            log.warn("Can not find nodes 'by_property' in project id: {}, because it does not exist", projectId);
            return ResponseEntity.badRequest().body("invalid_project_id");
        }

        final var nodes = classNodeService.findByPropertyValue(projectId, propertyKey, propertyValue);

        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/all/{user}/{project}")
    ResponseEntity<List<?>> getAllClassNodesInProject(@PathVariable String user, @PathVariable String project)
    {
        return null;
    }
}