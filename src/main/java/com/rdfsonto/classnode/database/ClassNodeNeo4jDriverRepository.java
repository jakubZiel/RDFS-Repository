package com.rdfsonto.classnode.database;

import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.ADD_LABEL_TO_ALL_NODES_WITH_ID_IN_NODE_IDS;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.ALL_INCOMING_NEIGHBOURS_QUERY_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.ALL_OUTGOING_NEIGHBOURS_QUERY_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.AND;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.CLEAR_PROPERTIES_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.COUNT_NODE_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.CREATE_NODE_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.DELETE_ALL_RESOURCE_NODES_WITH_LABEL_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.FILTER_BY_NODE_IDS;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.FIND_ALL_NODE_PROPERTIES_BY_PROJECT_LABEL_QUERY_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.FIND_ALL_NODE_PROPERTIES_QUERY_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.INCOMING_NEIGHBOURS_QUERY_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.MATCH_NODE_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.NEIGHBOUR_RECORD_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.NODE_IDS_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.NODE_ID_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.NODE_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.OUTGOING_NEIGHBOURS_QUERY_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.PATTERN_MATCHING_ANY_LINK;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.PATTERN_MATCHING_INCOMING_LINK;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.PATTERN_MATCHING_OUTGOING_LINK;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.RELATIONSHIP_ID_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.RELATION_RECORD_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.RETURN_NODE_ID;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.SET_NODE_PROPERTIES_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.SET_PROPERTY_TEMPLATE;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.SOURCE_NODE_ID_RECORD_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.URI_KEY;
import static com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepositoryTemplates.WITH_NODE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.util.Strings;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.FilterCondition;
import com.rdfsonto.classnode.service.PatternFilter;
import com.rdfsonto.util.database.PaginationClause;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
@RequiredArgsConstructor
public class ClassNodeNeo4jDriverRepository
{
    private final static String EMPTY_COMMAND = "";
    private final static String TARGET_DATABASE = "neo4j";
    private final Driver driver;
    private final Neo4jTemplate neo4jTemplate;
    private final Neo4jClient neo4jClient;
    private final RelationshipNeo4jDriverRepository relationshipNeo4jDriverRepository;
    private final ClassNodeRepository classNodeRepository;

    private final ClassNodeVoMapper classNodeVoMapper;
    private final ClassNodePropertiesVoMapper classNodePropertiesVoMapper;
    private final RelationshipVoMapper relationshipVoMapper;

    public ClassNodePropertiesProjection save(final ClassNode updateNode)
    {
        final var transaction = driver.session().beginTransaction();
        try
        {
            final var nonNullId = Optional.ofNullable(updateNode.id()).map(List::of).orElse(List.of());

            final var nodeId = classNodeRepository.findAllByIdIn(nonNullId).stream()
                .findFirst()
                .map(ClassNodePropertiesProjection::getId)
                .orElseGet(() -> create(updateNode, transaction).getId());

            final Set<RelationshipVo> incomingLinks = findAllIncomingNeighbours(List.of(nodeId), false).stream()
                .map(nodeConnection -> relationshipVoMapper.mapToVo(nodeConnection, updateNode.id(), true))
                .collect(Collectors.toSet());

            final Set<RelationshipVo> outgoingLinks = findAllOutgoingNeighbours(List.of(nodeId), false).stream()
                .map(nodeConnection -> relationshipVoMapper.mapToVo(nodeConnection, updateNode.id(), false))
                .collect(Collectors.toSet());

            handleLabelsDiff(updateNode, nodeId, transaction);
            handlePropertiesDiff(updateNode, nodeId, transaction);
            handleRelationshipDiff(nodeId, true, updateNode.incomingNeighbours(), incomingLinks, transaction);
            handleRelationshipDiff(nodeId, false, updateNode.outgoingNeighbours(), outgoingLinks, transaction);

            transaction.commit();

            return classNodeRepository.findAllByIdIn(List.of(nodeId)).stream()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Class node with ID: %s is not found after after being saved.".formatted(nodeId)));
        }
        catch (final Exception exception)
        {
            transaction.rollback();
            throw exception;
        }
    }

    public List<ClassNodeVo> findAllIncomingNeighbours(final List<Long> ids, final boolean relationOnlyBetweenFetched)
    {
        return findNeighbours(ids, RelationshipDirection.INCOMING, relationOnlyBetweenFetched);
    }

    public List<ClassNodeVo> findAllOutgoingNeighbours(final List<Long> ids, final boolean relationOnlyBetweenFetched)
    {
        return findNeighbours(ids, RelationshipDirection.OUTGOING, relationOnlyBetweenFetched);
    }

    public Map<Long, Map<String, Object>> findAllNodeProperties(final List<Long> ids)
    {
        try (final var session = driver.session())
        {
            final var paramMap = Map.of(NODE_IDS_KEY, (Object) ids);
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
    public List<Long> findAllNodeIdsByPropertiesAndLabels(final List<String> labels,
                                                          final List<FilterCondition> propertyFilters,
                                                          final Pageable page)
    {
        final var paginationClause = new PaginationClause(page);

        try (final var session = driver.session())
        {
            final var matchClause = buildMatchClause(labels);

            final var whereClause = buildWhereClause(propertyFilters);
            final var query = matchClause + whereClause + "RETURN node " + paginationClause.createPaginationClause();

            final var queryResult = session.run(query);
            return queryResult.list(record -> record.get(NODE_KEY).asNode().id());
        }
        catch (final Neo4jException exception)
        {
            throw new IllegalStateException(exception.getMessage());
        }
    }

    public long countNodeIdsByPropertiesAndLabels(final List<String> labels,
                                                  final List<FilterCondition> propertyFilters)
    {
        try (final var session = driver.session())
        {
            final var matchClause = buildMatchClause(labels);

            final var whereClause = buildWhereClause(propertyFilters);
            final var query = matchClause + whereClause + "RETURN count(node)";

            final var queryResult = session.run(query);

            return queryResult.single().get(COUNT_NODE_KEY).asLong();
        }
        catch (final Neo4jException exception)
        {
            throw new IllegalStateException(exception.getMessage());
        }
    }

    // TODO - does not return nodes that are 'outside ready', meaning have unique uri's, use UniqueUriHandler in elasticSearch
    public List<ClassNodeVo> findAllByProject(final String projectLabel, final Pageable page)
    {
        final var paginationClause = new PaginationClause(page).createPaginationClause();
        final var query = FIND_ALL_NODE_PROPERTIES_BY_PROJECT_LABEL_QUERY_TEMPLATE.formatted(projectLabel) + " " + paginationClause;

        return neo4jClient.query(query)
            .fetchAs(ClassNodeVo.class)
            .mappedBy((system, record) -> classNodeVoMapper.mapToVo(record)).all()
            .stream()
            .toList();
    }

    public void deleteAllNodesByProjectLabel(final String projectLabel)
    {
        try
        {
            final var query = DELETE_ALL_RESOURCE_NODES_WITH_LABEL_TEMPLATE.formatted(projectLabel);
            driver.session().run(query);
        }
        catch (final Exception exception)
        {
            log.error("Failed to completely delete an ontology with projectLabel: %s, should be handled by DBA.".formatted(projectLabel));
        }
    }

    @Transactional
    public void batchAddLabel(final List<Long> nodeIds, final String projectTag)
    {
        final var query = ADD_LABEL_TO_ALL_NODES_WITH_ID_IN_NODE_IDS.formatted(projectTag);
        final var parameters = Map.of(NODE_IDS_KEY, (Object) nodeIds);

        neo4jTemplate.toExecutableQuery(ClassNodeVo.class, new QueryFragmentsAndParameters(query, parameters)).getResults();
    }

    @Transactional
    public List<Long> findByPattern(final List<PatternFilter> patternFilters, final String projectLabel, final List<Long> nodeIds)
    {
        if (patternFilters == null || patternFilters.isEmpty())
        {
            return nodeIds;
        }

        final var patterns = patternFilters.stream()
            .map(pattern -> switch (pattern.getDirection())
            {
                case OUTGOING -> PATTERN_MATCHING_OUTGOING_LINK.formatted(projectLabel, pattern.getRelationshipName());
                case INCOMING -> PATTERN_MATCHING_INCOMING_LINK.formatted(projectLabel, pattern.getRelationshipName());
                case ANY -> PATTERN_MATCHING_ANY_LINK.formatted(projectLabel, pattern.getRelationshipName());
            })
            .collect(Collectors.joining(WITH_NODE));

        final var filterByNodeId = nodeIds.isEmpty() ? EMPTY_COMMAND : FILTER_BY_NODE_IDS;

        final var query = patterns + filterByNodeId + RETURN_NODE_ID;

        return neo4jClient.query(query)
            .bind(nodeIds).to(NODE_IDS_KEY)
            .fetchAs(Long.class)
            .mappedBy((typeSystem, record) -> record.get(NODE_ID_KEY).asLong())
            .all().stream()
            .toList();
    }

    private ClassNodeVo create(final ClassNode node, final Transaction transaction)
    {
        final var paramMap = Map.of(URI_KEY, (Object) node.uri());
        final var result = transaction.run(CREATE_NODE_TEMPLATE, paramMap).single();

        return classNodeVoMapper.mapToVo(result.get(NODE_KEY).asNode(), null, null, null);
    }

    private List<ClassNodeVo> findNeighbours(final List<Long> ids,
                                             final RelationshipDirection relationshipDirection,
                                             final boolean relationOnlyBetweenFetched)
    {
        final var query = relationshipDirection == RelationshipDirection.INCOMING ?
            relationOnlyBetweenFetched ? INCOMING_NEIGHBOURS_QUERY_TEMPLATE : ALL_INCOMING_NEIGHBOURS_QUERY_TEMPLATE :
            relationOnlyBetweenFetched ? OUTGOING_NEIGHBOURS_QUERY_TEMPLATE : ALL_OUTGOING_NEIGHBOURS_QUERY_TEMPLATE;

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

        final var concatenatedLabels = labels.stream()
            .map("`%s`"::formatted)
            .collect(Collectors.joining(":"));

        return "MATCH (node:Resource:%s)\n".formatted(concatenatedLabels);
    }

    private String buildWhereClause(final List<FilterCondition> propertyFilters)
    {
        if (propertyFilters.isEmpty())
        {
            return Strings.EMPTY;
        }

        final var whereClause = "WHERE ";

        final var propertiesConditionStatement = propertyFilters.stream()
            .map(filter -> "%s %s ".formatted(multiValToSingleVal(filter.property()), buildOperatorAndValue(filter)))
            .collect(Collectors.joining(AND + Strings.LINE_SEPARATOR));

        return String.join(Strings.LINE_SEPARATOR, List.of(whereClause, propertiesConditionStatement)) + " \n";
    }

    private String buildOperatorAndValue(final FilterCondition filterCondition)
    {
        return switch (filterCondition.operator())
        {
            case EQUALS -> "= '%s'".formatted(filterCondition.value());
            case EXISTS -> "IS NOT NULL";
            default -> "%s '%s'".formatted(filterCondition.operator().toString(), filterCondition.value());
        };
    }

    private String multiValToSingleVal(final String property)
    {
        return property.equals("uri") ? "node.uri" : "apoc.text.join(node.`%s`, ' ')".formatted(property);
    }

    private void handleRelationshipDiff(final long nodeId,
                                        final boolean isIncoming,
                                        final Map<Long, List<String>> updateLinks,
                                        final Set<RelationshipVo> originalLinks,
                                        final Transaction transaction)
    {
        final var update = Optional.ofNullable(updateLinks)
            .orElse(Collections.emptyMap())
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

        final var clearPropsStatement = String.join(Strings.LINE_SEPARATOR,
            List.of(SET_NODE_PROPERTIES_TEMPLATE.formatted(nodeId),
                CLEAR_PROPERTIES_TEMPLATE));

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
        final var matchAndSetPropertiesComponents =
            List.of(SET_NODE_PROPERTIES_TEMPLATE.formatted(nodeId), setPropertiesStatement);
        final var matchAndSetPropertiesStatement = String.join(Strings.LINE_SEPARATOR, matchAndSetPropertiesComponents);

        final var paramMap = IntStream.range(0, nodeUpdate.properties().size())
            .mapToObj(index -> Pair.of("param" + index, properties.get(index).getSecond()))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

        final var updatePropertiesQuery = new Query(matchAndSetPropertiesStatement, paramMap);

        transaction.run(updatePropertiesQuery);
    }

    private void handleLabelsDiff(final ClassNode nodeUpdate, final long nodeId, final Transaction transaction)
    {
        final Map<String, Object> nodeIdParamMap = Map.of(NODE_ID_KEY, nodeId);
        final var queryNodeLabels =
            new Query(MATCH_NODE_TEMPLATE + "RETURN labels(node) AS labels", nodeIdParamMap);

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
        final var updateLabelsQuery = String.join(Strings.LINE_SEPARATOR, updateLabelsQueryComponents);

        final var query = new Query(updateLabelsQuery, nodeIdParamMap);

        transaction.run(query);
    }
}