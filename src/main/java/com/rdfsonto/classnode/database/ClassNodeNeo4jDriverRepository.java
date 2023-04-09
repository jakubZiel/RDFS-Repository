package com.rdfsonto.classnode.database;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.util.Strings;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.FilterCondition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
@RequiredArgsConstructor
public class ClassNodeNeo4jDriverRepository
{
    private static final String MATCH_NODE_TEMPLATE = """
        MATCH (node:Resource) where id(node) = $nodeId
        """;
    private static final String CREATE_NODE_TEMPLATE = """
        CREATE (node:Resource{uri: $uri}) return node
        """;
    private static final String CLEAR_PROPERTIES_TEMPLATE = """
        node = {uri: $uri}
        """;

    private static final String SET_NODE_PROPERTIES_TEMPLATE = """
        MATCH (node) where id(node) = %s
        SET""";
    private static final String SET_PROPERTY_TEMPLATE = """
        node.`%s` = $param%s""";

    private static final String OUTGOING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)-[rel]->(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, id(n) as source, type(rel) as relation, id(rel) as relationshipId
        """;

    private static final String INCOMING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)<-[rel]-(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, id(n) as source, type(rel) as relation, id(rel) as relationshipId
        """;

    private static final String FIND_ALL_NODE_PROPERTIES_QUERY_TEMPLATE = """
        UNWIND $nodeIds AS nodeId
        MATCH (n:Resource) WHERE id(n) = nodeId
        RETURN id(n) as id, properties(n) as properties
        """;

    private static final String NEIGHBOUR_RECORD_KEY = "neighbour";
    private static final String RELATION_RECORD_KEY = "relation";
    private static final String SOURCE_NODE_ID_RECORD_KEY = "source";
    private static final String NODE_KEY = "node";
    private static final String NODE_IDS_KEY = "nodeIds";
    private static final String NODE_ID_KEY = "nodeId";
    private static final String URI_KEY = "uri";
    private static final String RELATIONSHIP_ID_KEY = "relationshipId";
    private static final String AND = "AND";

    private final Driver driver;
    private final RelationshipNeo4jDriverRepository relationshipNeo4jDriverRepository;
    private final ClassNodeRepository classNodeRepository;

    private final ClassNodeVoMapper classNodeVoMapper;
    private final ClassNodePropertiesVoMapper classNodePropertiesVoMapper;
    private final RelationshipVoMapper relationshipVoMapper;

    public ClassNodeProjection save(final ClassNode updateNode)
    {
        final var transaction = driver.session().beginTransaction();

        try
        {
            final var nodeId = classNodeRepository.findProjectionById(updateNode.id())
                .map(ClassNodeProjection::getId)
                .orElseGet(() -> create(updateNode, transaction).getId());

            final Set<RelationshipVo> incomingLinks = findAllIncomingNeighbours(List.of(nodeId)).stream()
                .map(nodeConnection -> relationshipVoMapper.mapToVo(nodeConnection, updateNode.id(), true))
                .collect(Collectors.toSet());

            final Set<RelationshipVo> outgoingLinks = findAllOutgoingNeighbours(List.of(nodeId)).stream()
                .map(nodeConnection -> relationshipVoMapper.mapToVo(nodeConnection, updateNode.id(), false))
                .collect(Collectors.toSet());

            handleLabelsDiff(updateNode, nodeId, transaction);
            handlePropertiesDiff(updateNode, nodeId, transaction);
            handleRelationshipDiff(nodeId, true, updateNode.incomingNeighbours(), incomingLinks, transaction);
            handleRelationshipDiff(nodeId, false, updateNode.outgoingNeighbours(), outgoingLinks, transaction);

            transaction.commit();

            // TODO make it inside commit
            return classNodeRepository.findProjectionById(nodeId)
                .orElseThrow(() -> new IllegalStateException("Class node with ID: %s is not found after after being saved.".formatted(nodeId)));
        }
        catch (final Exception exception)
        {
            transaction.rollback();
            throw exception;
        }
    }

    public ClassNodeVo create(final ClassNode node, final Transaction transaction)
    {
        final var paramMap = Map.of(URI_KEY, (Object) node.uri());
        final var result = transaction.run(CREATE_NODE_TEMPLATE, paramMap).single();

        return classNodeVoMapper.mapToVo(result.get(NODE_KEY).asNode(), null, null, null);
    }

    public List<ClassNodeVo> findAllIncomingNeighbours(final List<Long> ids)
    {
        return findNeighbours(ids, RelationshipDirection.INCOMING);
    }

    public List<ClassNodeVo> findAllOutgoingNeighbours(final List<Long> ids)
    {
        return findNeighbours(ids, RelationshipDirection.OUTGOING);
    }

    public Map<Long, Map<String, Object>> findAllNodeProperties(final List<Long> ids)
    {
        try (final var session = driver.session())
        {
            final var paramMap = Map.of("nodeIds", (Object) ids);
            final var queryResult = session.run(FIND_ALL_NODE_PROPERTIES_QUERY_TEMPLATE, paramMap);

            return queryResult.stream()
                .map(classNodePropertiesVoMapper::mapToVo)
                .collect(Collectors.toMap(ClassNodePropertiesVo::nodeId, ClassNodePropertiesVo::properties));
        }
        catch (final Neo4jException exception)
        {
            log.error("Failed to fetch all properties for nodes with ids: {}, {}", ids, exception.getMessage());
            return null;
        }
    }

    // TODO
    public List<Long> findAllNodeIdsByPropertiesAndLabels(final List<String> labels, final List<FilterCondition> propertyFilters)
    {
        try (final var session = driver.session())
        {
            final var matchClause = buildMatchClause(labels);

            final var whereClause = buildWhereClause(propertyFilters);
            final var query = matchClause + whereClause + "RETURN node";

            final var queryResult = session.run(query);

            return queryResult.list((record -> record.get(NODE_KEY).asNode().id()));
        }
        catch (final Neo4jException exception)
        {
            return null;
        }
    }

    private List<ClassNodeVo> findNeighbours(final List<Long> ids, final RelationshipDirection relationshipDirection)
    {
        final var query = relationshipDirection == RelationshipDirection.INCOMING ?
            INCOMING_NEIGHBOURS_QUERY_TEMPLATE : OUTGOING_NEIGHBOURS_QUERY_TEMPLATE;

        if (ids == null || ids.isEmpty())
        {
            return List.of();
        }

        try (final var session = driver.session())
        {
            final var paramMap = Map.of(NODE_IDS_KEY, (Object) ids);
            final var queryResult = session.run(query, paramMap);

            return queryResult.list(record -> classNodeVoMapper.mapToVo(
                record.get(NEIGHBOUR_RECORD_KEY).asNode(),
                record.get(RELATION_RECORD_KEY).asString(),
                record.get(SOURCE_NODE_ID_RECORD_KEY).asLong(),
                record.get(RELATIONSHIP_ID_KEY).asLong()));
        }
        catch (final Neo4jException exception)
        {
            log.error("Failed to fetch for outgoing neighbours for nodes with ids: {}, {}", ids, exception.getMessage());
            return null;
        }
    }

    private String buildMatchClause(final List<String> labels)
    {
        if (labels.isEmpty())
        {
            return "MATCH (node:Resource)\n";
        }

        final var concatenatedLabels = labels.stream().reduce("", (matchStatement, label) -> matchStatement + ":%s".formatted(label));
        return "MATCH (node:Resource%s)\n".formatted(concatenatedLabels);
    }

    private String buildWhereClause(final List<FilterCondition> propertyFilters)
    {
        if (propertyFilters.isEmpty())
        {
            return "";
        }

        final var whereClause = new StringBuilder("WHERE ");

        propertyFilters.forEach(filter -> {
            final var condition = "node.`%s` %s '%s' %s".formatted(filter.property(), filter.operator(), filter.value(), AND);
            whereClause.append("%s \n".formatted(condition));
        });

        final int unusedAndIndex = whereClause.lastIndexOf(AND);
        whereClause.delete(unusedAndIndex, unusedAndIndex + AND.length());

        return whereClause.toString();
    }

    private void handleRelationshipDiff(final long nodeId,
                                        final boolean isIncoming,
                                        final Map<Long, List<String>> updateLinks,
                                        final Set<RelationshipVo> originalLinks,
                                        final Transaction transaction)
    {
        final var update = updateLinks
            .entrySet().stream()
            .flatMap(neighbour -> neighbour.getValue().stream()
                .map(relationship -> RelationshipVo.builder()
                    .withRelationship(relationship)
                    .withDestinationId(isIncoming ? nodeId : neighbour.getKey())
                    .withSourceId(isIncoming ? neighbour.getKey() : nodeId)
                    .build()))
            .collect(Collectors.toSet());

        final var toDelete = originalLinks.stream()
            .filter(incoming -> !update.contains(incoming))
            .toList();

        final var toCreate = update.stream()
            .filter(incoming -> !originalLinks.contains(incoming))
            .toList();

        relationshipNeo4jDriverRepository.save(toCreate, transaction);
        relationshipNeo4jDriverRepository.delete(toDelete, transaction);
    }

    private void handlePropertiesDiff(final ClassNode nodeUpdate, final long nodeId, final Transaction transaction)
    {
        final var uriParam = Map.of(URI_KEY, (Object) nodeUpdate.uri());

        final var clearPropsStatement = Strings.join(
            List.of(SET_NODE_PROPERTIES_TEMPLATE.formatted(nodeId),
                CLEAR_PROPERTIES_TEMPLATE), '\n');

        final var clearPropsQuery = new Query(clearPropsStatement, uriParam);
        transaction.run(clearPropsQuery);

        if (nodeUpdate.properties().isEmpty())
        {
            return;
        }

        final var properties = nodeUpdate.properties().entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();

        final var setPropertiesComponents = IntStream.range(0, nodeUpdate.properties().size())
            .mapToObj(index -> SET_PROPERTY_TEMPLATE.formatted(properties.get(index).getFirst(), index))
            .toList();

        final var setPropertiesStatement = Strings.join(setPropertiesComponents, ',');
        final var matchAndSetPropertiesComponents = List.of(SET_NODE_PROPERTIES_TEMPLATE.formatted(nodeId), setPropertiesStatement);
        final var matchAndSetPropertiesStatement = Strings.join(matchAndSetPropertiesComponents, '\n');

        final var paramMap = IntStream.range(0, nodeUpdate.properties().size())
            .mapToObj(index -> Pair.of("param" + index, properties.get(index).getSecond()))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

        final var updatePropertiesQuery = new Query(matchAndSetPropertiesStatement, paramMap);

        transaction.run(updatePropertiesQuery);
    }

    private void handleLabelsDiff(final ClassNode nodeUpdate, final long nodeId, final Transaction transaction)
    {
        final Map<String, Object> nodeIdParamMap = Map.of(NODE_ID_KEY, nodeId);
        final var queryNodeLabels = new Query(MATCH_NODE_TEMPLATE + "return labels(node) as labels", nodeIdParamMap);

        final var persistedLabels = transaction.run(queryNodeLabels).single().get("labels").asList(Value::asString).stream()
            .map("`%s`"::formatted)
            .toList();

        final var updateLabels = nodeUpdate.classLabels().stream()
            .map("`%s`"::formatted)
            .distinct()
            .toList();

        final var setQuery = "SET node:" + Strings.join(updateLabels, ':');
        final var removeQuery = "REMOVE node:" + Strings.join(persistedLabels, ':');

        final var updateLabelsQueryComponents = List.of(MATCH_NODE_TEMPLATE, removeQuery, setQuery);
        final var updateLabelsQuery = Strings.join(updateLabelsQueryComponents, '\n');

        final var query = new Query(updateLabelsQuery, nodeIdParamMap);

        transaction.run(query);
    }
}