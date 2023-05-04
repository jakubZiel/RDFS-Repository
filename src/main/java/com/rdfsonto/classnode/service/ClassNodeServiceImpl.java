package com.rdfsonto.classnode.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_MAX_DISTANCE;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_NODE_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_NODE_URI;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PROJECT_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_REQUEST;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.database.ClassNodeProjection;
import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.database.ClassNodeVo;
import com.rdfsonto.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(noRollbackFor = ClassNodeException.class)
public class ClassNodeServiceImpl implements ClassNodeService
{
    private final static long MAX_NUMBER_OF_NEIGHBOURS = 1000;
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ClassNodeMapper classNodeMapper;
    private final ProjectService projectService;
    private final UriUniquenessHandler uriHandler;
    private final ClassNodeValidator classNodeValidator;

    @Override
    public List<ClassNode> findByIds(final List<Long> ids)
    {
        final var projectedNodes = classNodeRepository.findAllByIdIn(ids);

        if (projectedNodes.size() != ids.size())
        {
            throw new IllegalStateException("Not all nodes exist");
        }

        final var properties = classNodeNeo4jDriverRepository.findAllNodeProperties(ids);
        // TODO : Check if it works for ids : 11, 7, 3 ,1
        final var incoming = classNodeNeo4jDriverRepository.findAllIncomingNeighbours(ids);
        final var outgoing = classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(ids);

        final var groupedIncoming = incoming.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));
        final var groupedOutgoing = outgoing.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));

        final var notHydratedNodes = projectedNodes.stream()
            .map(projectedNode -> ClassNodeVo.builder()
                .withId(projectedNode.getId())
                .withUri(projectedNode.getUri())
                .withClassLabels(projectedNode.getClassLabels())
                .build())
            .toList();

        notHydratedNodes.forEach(node -> {
            final var props = properties.get(node.getId());
            node.setProperties(props);
        });

        return notHydratedNodes.stream()
            .map(node ->
                classNodeMapper.mapToDomain(node,
                    groupedIncoming.get(node.getId()),
                    groupedOutgoing.get(node.getId())))
            .map(uriHandler::removeUniqueness)
            .toList();
    }

    @Override
    public List<ClassNode> findByPropertyValue(final long projectId, final String propertyKey, final String value)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new IllegalStateException("Project id: %s does not exist".formatted(projectId)));

        final var projectTag = projectService.getProjectTag(project);

        final var nodeIds = classNodeRepository.findAllClassNodesByPropertyValue(propertyKey, value, projectTag).stream()
            .map(ClassNodeVo::getId)
            .toList();

        return findByIds(nodeIds);
    }

    @Override
    public List<ClassNode> findByPropertiesAndLabels(final long projectId, final List<String> labels, final List<FilterCondition> filters)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException(
                "Project with ID: %s does not exist, can not filter nodes.".formatted(projectId),
                INVALID_PROJECT_ID));

        final var projectTag = projectService.getProjectTag(project);
        final var uniqueFilters = uriHandler.applyUniqueness(filters, projectTag);
        final var uniqueLabels = uriHandler.addUniqueLabel(labels, projectTag);

        final var nodeIds = classNodeNeo4jDriverRepository.findAllNodeIdsByPropertiesAndLabels(uniqueLabels, uniqueFilters);

        return findByIds(nodeIds);
    }

    @Override
    public Optional<ClassNode> findById(final Long id)
    {
        final var notHydratedNodeProjection = classNodeRepository.findProjectionById(id);

        if (notHydratedNodeProjection.isEmpty())
        {
            return Optional.empty();
        }

        final var notHydratedNode = ClassNodeVo.builder()
            .withId(notHydratedNodeProjection.get().getId())
            .withUri(notHydratedNodeProjection.get().getUri())
            .withClassLabels(notHydratedNodeProjection.get().getClassLabels())
            .build();

        final var properties = classNodeNeo4jDriverRepository.findAllNodeProperties(List.of(id));

        notHydratedNode.setProperties(properties.get(id));

        final var nodeId = List.of(id);
        final var incoming = classNodeNeo4jDriverRepository.findAllIncomingNeighbours(nodeId);
        final var outgoing = classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(nodeId);

        return Optional.of(classNodeMapper.mapToDomain(notHydratedNode, incoming, outgoing))
            .map(uriHandler::removeUniqueness);
    }

    @Override
    public List<ClassNode> findNeighboursByUri(final String nodeUri,
                                               final long projectId,
                                               final int maxDistance,
                                               final List<String> allowedRelationships)
    {
        projectService.findById(projectId)
            .orElseThrow(() ->
                new ClassNodeException(
                    "Project with ID: %s does not exist, can't look for uri: %s nodeUri".formatted(projectId, nodeUri),
                    INVALID_PROJECT_ID));

        final var sourceNode = classNodeRepository.findByUri(nodeUri).orElseThrow(() ->
            new ClassNodeException(
                "Node with URI: %s does not exist in project with ID: %s".formatted(nodeUri, projectId),
                INVALID_NODE_URI));

        return findNeighbours(sourceNode.getId(), maxDistance, allowedRelationships);
    }

    @Override
    public List<ClassNode> findNeighbours(final long id, final int maxDistance, final List<String> allowedRelationships)
    {
        classNodeRepository.findProjectionById(id)
            .orElseThrow(() -> new ClassNodeException("Tried to get neighbours of non existing node with ID: %s".formatted(id), INVALID_NODE_ID));

        if (maxDistance < 0)
        {
            throw new ClassNodeException("Invalid max distance: %d".formatted(maxDistance), INVALID_MAX_DISTANCE);
        }

        final var numberOfNeighbours = classNodeRepository.countAllNeighbours(maxDistance, id);

        if (numberOfNeighbours > MAX_NUMBER_OF_NEIGHBOURS)
        {
            log.warn("Handling more than {} number of neighbours", numberOfNeighbours);
            throw new NotImplementedException();
        }

        final var neighbourIds = classNodeRepository.findAllNeighbours(maxDistance, id).stream()
            .map(ClassNodeProjection::getId)
            .toList();

        return findByIds(neighbourIds);
    }

    // TODO take care of more types of relationship coming from the same node - TEST IT!!!
    @Override
    public ClassNode save(final ClassNode nodeToSave, final long projectId)
    {
        classNodeValidator.validate(nodeToSave);

        final var project = projectService.findById(projectId)
            .orElseThrow(() ->
                new ClassNodeException("Can not save class node in non-existing project with ID: %s".formatted(projectId),
                    INVALID_PROJECT_ID));

        final var uniqueNode = uriHandler.applyUniqueness(nodeToSave, projectService.getProjectTag(project));

        Optional.of(uniqueNode)
            .filter(node -> node.id() == null)
            .filter(node -> classNodeRepository.findByUri(node.uri()).isPresent())
            .ifPresent(duplicateUriNode -> {
                throw new ClassNodeException("Attempted to create node with already existing URI: %s".formatted(duplicateUriNode.uri()),
                    INVALID_REQUEST);
            });

        final var persistedNode = classNodeNeo4jDriverRepository.save(uniqueNode);

        return findById(persistedNode.getId())
            .orElseThrow(() -> new IllegalStateException("Class node with ID: %s is not found after after being saved.".formatted(persistedNode.getId())));
    }

    @Override
    public void deleteById(final long id)
    {
        classNodeRepository.findProjectionById(id)
            .orElseThrow(() ->
                new ClassNodeException("Class node with ID: %s can not be deleted, because it does not exist.".formatted(id),
                    INVALID_NODE_ID));

        classNodeRepository.deleteById(id);
    }

    @Override
    public void deleteAll(final long projectId)
    {
        final var project = projectService.findById(projectId).orElseThrow();
        final var projectTag = projectService.getProjectTag(project);

        final var classNodeLabel = uriHandler.getClassNodeLabel(projectTag);

        classNodeRepository.deleteAllByClassLabels(List.of(classNodeLabel));
        // TODO validate that deleted all class nodes for a project
    }

    @Override
    public ProjectNodeMetadata findProjectNodeMetaData(final long projectId)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException("Not mete data for non existing project with ID: %s".formatted(projectId), INVALID_PROJECT_ID));

        final var projectTag = projectService.getProjectTag(project);
        final var projectLabel = uriHandler.getClassNodeLabel(projectTag);

        final var propertyKeys = classNodeRepository.findAllPropertyKeys(projectLabel).stream()
            .filter(property -> property.startsWith("http") || property.equals("uri"))
            .toList();

        final var labels = classNodeRepository.findAllLabels(projectTag).stream()
            .filter(label -> label.startsWith("http"))
            .toList();

        final var relationshipTypes = classNodeRepository.findAllRelationshipTypes(projectTag).stream()
            .filter(relationship -> relationship.startsWith("http"))
            .toList();

        return ProjectNodeMetadata.builder()
            .withPropertyKeys(propertyKeys)
            .withRelationshipTypes(relationshipTypes)
            .withNodeLabels(labels)
            .build();
    }
}