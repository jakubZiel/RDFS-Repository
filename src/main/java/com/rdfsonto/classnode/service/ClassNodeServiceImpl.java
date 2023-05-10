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

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
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
    private static final String USER_NAMESPACE_LABEL_PREFIX = "http://www.user_neo4j.com";
    private final static long MAX_NUMBER_OF_NEIGHBOURS = 1000;

    private final ProjectService projectService;
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ClassNodeMapper classNodeMapper;
    private final UriUniquenessHandler uriHandler;
    private final PrefixHandler prefixHandler;
    private final RemovePrefixHandler removePrefixHandler;
    private final ClassNodeValidator classNodeValidator;

    @Override
    public List<ClassNode> findByIds(final long projectId, final List<Long> ids)
    {
        final var projectedNodes = classNodeRepository.findAllByIdIn(ids);

        if (projectedNodes.size() != ids.size())
        {
            throw new IllegalStateException("Not all nodes exist");
        }

        final var properties = classNodeNeo4jDriverRepository.findAllNodeProperties(ids);

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

        final var nonPrefixedNodes = notHydratedNodes.stream()
            .map(node ->
                classNodeMapper.mapToDomain(node,
                    groupedIncoming.get(node.getId()),
                    groupedOutgoing.get(node.getId())))
            .map(uriHandler::removeUniqueness)
            .toList();

        return prefixHandler.applyPrefix(nonPrefixedNodes, projectId);
    }

    @Override
    public List<ClassNode> findByPropertiesAndLabels(final long projectId, final List<String> labels, final List<FilterCondition> filters)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException(
                "Project with ID: %s does not exist, can not filter nodes.".formatted(projectId),
                INVALID_PROJECT_ID));

        final var nonPrefixedLabels = removePrefixHandler.removePrefix(labels, projectId);
        final var nonPrefixedProperties = removePrefixHandler.removePrefix(filters.stream().map(FilterCondition::property).toList(), projectId);
        final var nonPrefixedFilters = Streams.zip(filters.stream(), nonPrefixedProperties.stream(),
            (filter, prefixedProp) -> filter.toBuilder().withProperty(prefixedProp).build()).toList();

        final var projectTag = projectService.getProjectTag(project);
        final var uniqueFilters = uriHandler.applyUniqueness(nonPrefixedFilters, projectTag);
        final var uniqueLabels = uriHandler.addUniqueLabel(nonPrefixedLabels, projectTag);

        final var nodeIds = classNodeNeo4jDriverRepository.findAllNodeIdsByPropertiesAndLabels(uniqueLabels, uniqueFilters);

        return findByIds(projectId, nodeIds);
    }

    @Override
    public Optional<ClassNode> findById(final long projectId, final Long id)
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
            .map(uriHandler::removeUniqueness)
            .map(node -> prefixHandler.applyPrefix(node, projectId));
    }

    @Override
    public List<ClassNode> findNeighboursByUri(final long projectId,
                                               final String nodeUri,
                                               final int maxDistance,
                                               final List<String> allowedRelationships)
    {
        projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException(
                "Project with ID: %s does not exist, can't look for uri: %s nodeUri".formatted(projectId, nodeUri),
                INVALID_PROJECT_ID));

        final var sourceNode = classNodeRepository.findByUri(nodeUri).orElseThrow(() ->
            new ClassNodeException(
                "Node with URI: %s does not exist in project with ID: %s".formatted(nodeUri, projectId),
                INVALID_NODE_URI));

        final var nonPrefixedRelationships = removePrefixHandler.removePrefix(allowedRelationships, projectId);

        return findNeighbours(projectId, sourceNode.getId(), maxDistance, nonPrefixedRelationships);
    }

    @Override
    public List<ClassNode> findNeighbours(final long projectId, final long nodeId, final int maxDistance, final List<String> allowedRelationships)
    {
        classNodeRepository.findProjectionById(nodeId)
            .orElseThrow(() -> new ClassNodeException("Tried to get neighbours of non existing node with ID: %s".formatted(nodeId), INVALID_NODE_ID));

        if (maxDistance < 0)
        {
            throw new ClassNodeException("Invalid max distance: %d".formatted(maxDistance), INVALID_MAX_DISTANCE);
        }

        final var numberOfNeighbours = classNodeRepository.countAllNeighbours(maxDistance, nodeId);

        if (numberOfNeighbours > MAX_NUMBER_OF_NEIGHBOURS)
        {
            log.warn("Handling more than {} number of neighbours", numberOfNeighbours);
            throw new NotImplementedException();
        }

        // TODO apply relationships to findAllNeighbours
        final var nonPrefixedRelationships = removePrefixHandler.removePrefix(allowedRelationships, projectId);

        final var neighbourIds = classNodeRepository.findAllNeighbours(maxDistance, nodeId).stream()
            .map(ClassNodeProjection::getId)
            .toList();

        return findByIds(projectId, neighbourIds);
    }

    @Override
    public ClassNode save(final long projectId, final ClassNode nodeToSave)
    {
        classNodeValidator.validate(nodeToSave);

        final var project = projectService.findById(projectId)
            .orElseThrow(() ->
                new ClassNodeException("Can not save class node in non-existing project with ID: %s".formatted(projectId),
                    INVALID_PROJECT_ID));

        final var nonPrefixedNode = removePrefixHandler.removePrefix(nodeToSave, projectId);
        final var uniqueNode = uriHandler.applyUniqueness(nonPrefixedNode, projectService.getProjectTag(project));

        Optional.of(uniqueNode)
            .filter(node -> node.id() == null)
            .filter(node -> classNodeRepository.findByUri(node.uri()).isPresent())
            .ifPresent(duplicateUriNode -> {
                throw new ClassNodeException("Attempted to create node with already existing URI: %s".formatted(duplicateUriNode.uri()),
                    INVALID_REQUEST);
            });

        final var persistedNode = classNodeNeo4jDriverRepository.save(uniqueNode);

        return findById(projectId, persistedNode.getId())
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

    //TODO apply uniqueness
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
            .filter(label -> !label.startsWith(USER_NAMESPACE_LABEL_PREFIX))
            .filter(label -> label.startsWith("http"))
            .toList();

        final var relationshipTypes = classNodeRepository.findAllRelationshipTypes(projectTag).stream()
            .filter(relationship -> relationship.startsWith("http"))
            .toList();

        final var nonPrefixedMetadata = ProjectNodeMetadata.builder()
            .withPropertyKeys(propertyKeys)
            .withRelationshipTypes(relationshipTypes)
            .withNodeLabels(labels)
            .build();

        return prefixHandler.applyPrefix(nonPrefixedMetadata, projectId);
    }
}