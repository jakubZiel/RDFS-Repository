package com.rdfsonto.classnode.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_MAX_DISTANCE;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_NODE_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_NODE_URI;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PROJECT_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_REQUEST;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.database.ClassNodeProjection;
import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.database.ClassNodeVo;
import com.rdfsonto.elastic.service.ElasticSearchClassNode;
import com.rdfsonto.elastic.service.ElasticSearchClassNodeService;
import com.rdfsonto.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(noRollbackFor = ClassNodeException.class)
public class ClassNodeServiceImpl implements ClassNodeService
{
    private static final String URI_PROPERTY = "uri";
    private static final String USER_NAMESPACE_LABEL_PREFIX = "http://www.user_neo4j.com";
    private final static long MAX_NUMBER_OF_NEIGHBOURS = 1000;

    private final ProjectService projectService;
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ClassNodeMapper classNodeMapper;
    private final UriUniquenessHandler uriHandler;
    private final UriRemoveUniquenessHandler uriRemoveHandler;
    private final PrefixHandler prefixHandler;
    private final RemovePrefixHandler removePrefixHandler;
    private final ClassNodeValidator classNodeValidator;
    private final ElasticSearchClassNodeService elasticSearchClassNodeService;

    @Override
    public List<ClassNode> findByIds(final long projectId, final List<Long> ids)
    {
        final var projectedNodes = classNodeRepository.findAllByIdIn(ids);

        if (projectedNodes.size() != ids.size())
        {
            throw new IllegalStateException("Not all nodes exist");
        }
        final var properties = classNodeNeo4jDriverRepository.findAllNodeProperties(ids);

        final var incoming = classNodeNeo4jDriverRepository.findAllIncomingNeighbours(ids, true);
        final var outgoing = classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(ids, true);

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
            .map(uriRemoveHandler::removeUniqueness)
            .toList();

        return prefixHandler.applyPrefix(nonPrefixedNodes, projectId);
    }

    @Override
    public List<ClassNode> findByPropertiesAndLabels(final long projectId,
                                                     final List<String> labels,
                                                     final List<FilterCondition> filters,
                                                     final List<PatternFilter> patterns,
                                                     final Pageable pageable)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException(
                "Project with ID: %s does not exist, can not filter nodes.".formatted(projectId),
                INVALID_PROJECT_ID));

        final var nonPrefixedFilters = handleFilterPropertyPrefixes(filters, projectId);

        final var projectTag = projectService.getProjectTag(project);
        final var uniqueFilters = nonPrefixedFilters.stream().map(filter -> uriHandler.applyUniqueness(filter, projectTag)).toList();

        final var filteredNodeIds = elasticSearchClassNodeService.search(project.getOwnerId(), projectId, filters, labels, pageable).stream()
            .map(ElasticSearchClassNode::id)
            .toList();

        final var uniquePatterns = patterns == null ? List.of() : patterns.stream()
            .map(pattern -> pattern.toBuilder()
                .withRelationshipName(uriHandler.applyUniqueness(pattern.getRelationshipName(), projectTag, true))
                .build());

        final var nodeIds = classNodeNeo4jDriverRepository.findByPattern(patterns, uriHandler.getClassNodeLabel(projectTag), filteredNodeIds);

        return findByIdsLight(projectId, nodeIds);
    }

    @Override
    public List<ClassNode> findByProject(final long projectId, final Pageable page)
    {
        final var projectTag = projectService.findById(projectId)
            .map(projectService::getProjectTag)
            .map(uriHandler::getClassNodeLabel)
            .orElseThrow(() -> new IllegalStateException("Project with id: %s does not exist.".formatted(projectId)));

        final var noneUnique = classNodeNeo4jDriverRepository.findAllByProject(projectTag, page).stream()
            .map(node -> classNodeMapper.mapToDomain(node, null, null))
            .map(uriRemoveHandler::removeUniqueness)
            .toList();

        return prefixHandler.applyPrefix(noneUnique, projectId);
    }

    @Override
    public Optional<ClassNode> findById(final long projectId, final Long id)
    {
        final var notHydratedNodeProjection = classNodeRepository.findAllByIdIn(List.of(id)).stream().findFirst();

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
        final var incoming = classNodeNeo4jDriverRepository.findAllIncomingNeighbours(nodeId, false);
        final var outgoing = classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(nodeId, false);

        return Optional.of(classNodeMapper.mapToDomain(notHydratedNode, incoming, outgoing))
            .map(uriRemoveHandler::removeUniqueness)
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

        //TODO apply relationships to findAllNeighbours
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

        final var result = findById(projectId, persistedNode.getId())
            .orElseThrow(() -> new IllegalStateException("Class node with ID: %s is not found after after being saved.".formatted(persistedNode.getId())));

        elasticSearchClassNodeService.save(project.getOwnerId(), projectId, result);
        return result;
    }

    @Override
    public void deleteById(final long projectId, final long id)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new IllegalStateException("Can not delete a node from non-existing project id: %s.".formatted(projectId)));

        final var node = findByIdsLight(projectId, List.of(id)).stream().
            findAny()
            .orElseThrow(() ->
                new ClassNodeException("Class node with ID: %s can not be deleted, because it does not exist.".formatted(id), INVALID_NODE_ID));

        classNodeRepository.deleteById(id);
        elasticSearchClassNodeService.delete(project.getOwnerId(), projectId, node);
    }

    //TODO apply uniqueness
    @Override
    public ProjectNodeMetadata findProjectNodeMetaData(final long projectId)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException("Not mete data for non existing project with ID: %s".formatted(projectId), INVALID_PROJECT_ID));

        final var projectTag = projectService.getProjectTag(project);

        final var propertyKeys = classNodeRepository.findAllPropertyKeysFast(projectTag).stream()
            .filter(property -> property.startsWith("http") || property.equals("uri"))
            .sorted()
            .toList();

        final var labels = classNodeRepository.findAllLabels(projectTag).stream()
            .filter(label -> !label.startsWith(USER_NAMESPACE_LABEL_PREFIX))
            .filter(label -> label.startsWith("http"))
            .toList();

        final var relationshipTypes = classNodeRepository.findAllRelationshipTypes(projectTag).stream()
            .filter(relationship -> relationship.startsWith("http"))
            .sorted()
            .toList();

        final var uniqueMetaData = ProjectNodeMetadata.builder()
            .withPropertyKeys(propertyKeys)
            .withRelationshipTypes(relationshipTypes)
            .withNodeLabels(labels)
            .build();

        final var nonUniqueMetadata = uriRemoveHandler.removeUniqueness(uniqueMetaData);
        return prefixHandler.applyPrefix(nonUniqueMetadata, projectId);
    }

    @Override
    public List<ClassNode> findByIdsLight(final long projectId, final List<Long> ids)
    {
        final var projectedNodes = classNodeRepository.findAllByIdIn(ids);

        if (projectedNodes.size() != ids.size())
        {
            throw new IllegalStateException("Not all nodes exist");
        }
        final var properties = classNodeNeo4jDriverRepository.findAllNodeProperties(ids);

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
            .map(node -> classNodeMapper.mapToDomain(node, null, null))
            .map(uriRemoveHandler::removeUniqueness)
            .toList();

        return prefixHandler.applyPrefix(nonPrefixedNodes, projectId);
    }

    private List<FilterCondition> handleFilterPropertyPrefixes(final List<FilterCondition> filters, final long projectId)
    {
        final var uriFilterGroupByIsUri = filters.stream()
            .collect(Collectors.groupingBy(filter -> filter.property().equals(URI_PROPERTY)));

        final var uri = Optional.ofNullable(uriFilterGroupByIsUri.get(true)).orElse(List.of());
        final var nonUri = Optional.ofNullable(uriFilterGroupByIsUri.get(false)).orElse(List.of());

        final var nonPrefixedUriFilter = uri.stream()
            .map(filter -> filter.toBuilder()
                .withValue(removePrefixHandler.removePrefix(filter.value(), projectId))
                .build());

        final var nonUriFilterProperties = nonUri.stream().map(FilterCondition::property).toList();
        final var nonPrefixedNonUriFilterPropertiesStream = removePrefixHandler.removePrefix(nonUriFilterProperties, projectId).stream();

        final var nonPrefixedNonUriFilters = Streams.zip(nonPrefixedNonUriFilterPropertiesStream, nonUri.stream(),
            (nonPrefixedProperty, filter) -> filter.toBuilder().withProperty(nonPrefixedProperty).build());

        return Stream.concat(nonPrefixedUriFilter, nonPrefixedNonUriFilters).toList();
    }
}