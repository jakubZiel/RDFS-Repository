package com.rdfsonto.classnode.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_MAX_DISTANCE;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_NODE_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_NODE_URI;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PROJECT_ID;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.database.ClassNodeVo;
import com.rdfsonto.classnode.database.ClassNodeVoMapper;
import com.rdfsonto.classnode.database.LinkVo;
import com.rdfsonto.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class ClassNodeServiceImpl implements ClassNodeService
{
    private final static long MAX_NUMBER_OF_NEIGHBOURS = 1000;
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ClassNodeMapper classNodeMapper;
    private final ClassNodeVoMapper classNodeVoMapper;
    private final ProjectService projectService;
    private final UriUniquenessHandler uriHandler;

    @Override
    public List<ClassNode> findByIds(final List<Long> ids)
    {
        final var notHydratedNodes = classNodeRepository.findAllById(ids);

        if (notHydratedNodes.size() != ids.size())
        {
            throw new IllegalStateException("Not all nodes exist");
        }

        final var properties = classNodeNeo4jDriverRepository.findAllNodeProperties(ids);
        // TODO : Check if it works for ids : 11, 7, 3 ,1
        final var incoming = classNodeNeo4jDriverRepository.findAllIncomingNeighbours(ids);
        final var outgoing = classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(ids);

        final var groupedIncoming = incoming.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));
        final var groupedOutgoing = outgoing.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));

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

        // TODO apply filter for projectTags
        final var projectTag = projectService.getProjectTag(project);

        final var nodeIds = classNodeNeo4jDriverRepository.findAllNodeIdsByPropertiesAndLabels(labels, filters);

        return findByIds(nodeIds);
    }

    @Override
    public Optional<ClassNode> findById(final Long id)
    {
        final var notHydratedNode = classNodeRepository.findById(id);

        if (notHydratedNode.isEmpty())
        {
            return Optional.empty();
        }

        final var properties = classNodeNeo4jDriverRepository.findAllNodeProperties(List.of(id));

        notHydratedNode.ifPresent(x -> x.setProperties(properties.get(id)));

        final var incoming = classNodeRepository.findAllIncomingNeighbours(id);
        final var outgoing = classNodeRepository.findAllOutgoingNeighbours(id);

        return Optional.of(classNodeMapper.mapToDomain(notHydratedNode.get(), incoming, outgoing))
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
        classNodeRepository.findById(id)
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
            .map(ClassNodeVo::getId)
            .toList();

        return findByIds(neighbourIds);
    }

    // TODO take care of more types of relationship coming from the same node - TEST IT!!!
    @Override
    public ClassNode save(final ClassNode node, final long projectId)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() ->
                new ClassNodeException("Can not save class node in non-existing project with ID: %s".formatted(projectId),
                    INVALID_PROJECT_ID));

        final var uniqueNode = uriHandler.applyUniqueness(node, projectService.getProjectTag(project));

        final var nodeVo = classNodeVoMapper.mapToVo(uniqueNode);
        final var savedVo = classNodeRepository.save(nodeVo);

        final var updatedNode = handleIncoming(uniqueNode, savedVo);

        final var oldOutgoingIds = classNodeRepository.findAllOutgoingNeighbours(uniqueNode.id()).stream()
            .map(ClassNodeVo::getId)
            .toList();

        final var oldOutgoing = classNodeRepository.findAllById(oldOutgoingIds);

        final var updatedOutgoing = handleOutgoing(oldOutgoing, uniqueNode, savedVo);

        classNodeRepository.saveAll(updatedOutgoing);
        classNodeRepository.save(updatedNode);

        return findById(savedVo.getId())
            .orElseThrow(() -> new IllegalStateException("Class node with ID: %s is not found after after being saved.".formatted(savedVo.getId())));
    }

    @Override
    public void deleteById(final long id)
    {
        classNodeRepository.findById(id)
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

        final var propertyKeys = classNodeRepository.findAllPropertyKeys(projectTag).stream()
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

    private List<ClassNodeVo> handleOutgoing(final List<ClassNodeVo> outgoing, final ClassNode uniqueNode, final ClassNodeVo savedVo)
    {
        final var originalOutgoingLinks = outgoing.stream()
            .flatMap(outgoingNeighbour -> outgoingNeighbour.getNeighbours().entrySet().stream()
                .filter(neighboursByRelationship -> {
                    final var neighboursList = neighboursByRelationship.getValue();
                    return neighboursList.stream().anyMatch(neighbour -> neighbour.getId().equals(uniqueNode.id()));
                })
                .map(link -> LinkVo.builder()
                    .withSourceId(uniqueNode.id())
                    .withDestinationId(outgoingNeighbour.getId())
                    .withRelationship(link.getKey())
                    .build()))
            .toList();

        final var updateOutgoingLinks = uniqueNode.outgoingNeighbours().entrySet().stream()
            .flatMap(outgoingNeighbour -> outgoingNeighbour.getValue().stream()
                .map(relationship -> LinkVo.builder()
                    .withSourceId(uniqueNode.id())
                    .withDestinationId(outgoingNeighbour.getKey())
                    .withRelationship(relationship)
                    .build()))
            .toList();

        final var removedLinks = originalOutgoingLinks.stream()
            .filter(originalLink -> !updateOutgoingLinks.contains(originalLink))
            .toList();

        final var addedLinks = updateOutgoingLinks.stream()
            .filter(updateLink -> !originalOutgoingLinks.contains(updateLink))
            .toList();

        final var addedOutgoingIds = addedLinks.stream()
            .map(LinkVo::getDestinationId)
            .toList();

        final var oldOutgoingMap = outgoing.stream()
            .collect(Collectors.toMap(ClassNodeVo::getId, Function.identity()));
        final var addedOutgoingMap = classNodeRepository.findAllById(addedOutgoingIds).stream()
            .collect(Collectors.toMap(ClassNodeVo::getId, Function.identity()));

        final var mergedOutgoing = Stream.concat(oldOutgoingMap.entrySet().stream(), addedOutgoingMap.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

        removedLinks.forEach(removedLink -> mergedOutgoing.get(removedLink.getDestinationId()).getNeighbours()
            .get(removedLink.getRelationship()).removeIf(node -> node.getId().equals(uniqueNode.id())));

        addedLinks.forEach(addedLink -> mergedOutgoing.get(addedLink.getDestinationId()).getNeighbours()
            .get(addedLink.getRelationship()).add(savedVo));

        return mergedOutgoing.values().stream().toList();
    }

    private ClassNodeVo handleIncoming(final ClassNode uniqueNode, final ClassNodeVo savedVo)
    {
        final var incomingNeighboursMap = classNodeRepository.findAllById(uniqueNode.incomingNeighbours().keySet()).stream()
            .collect(Collectors.toMap(ClassNodeVo::getId, Function.identity()));

        final var updateIncomingLinks = uniqueNode.incomingNeighbours().entrySet().stream()
            .flatMap(incomingNeighbour -> incomingNeighbour.getValue().stream()
                .map(relationship -> LinkVo.builder()
                    .withRelationship(relationship)
                    .withDestinationId(uniqueNode.id())
                    .withSourceId(incomingNeighbour.getKey())
                    .build()))
            .collect(Collectors.groupingBy(LinkVo::getRelationship)).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                links -> links.getValue().stream().map(link -> incomingNeighboursMap.get(link.getSourceId())).toList()));

        savedVo.setNeighbours(updateIncomingLinks);

        return savedVo;
    }
}