package com.rdfsonto.classnode.database;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Transaction;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class RelationshipNeo4jDriverRepository
{
    private final Neo4jTemplate neo4jTemplate;

    @Builder(setterPrefix = "with", toBuilder = true)
    private record RelationshipId(long startNode, long endNode)
    {
    }

    private static final String RELATIONSHIP_IDS_PARAMETER = "relationshipIds";

    private static final String DELETE_RELATIONSHIPS_BY_ID = """
        MATCH ()-[relation]-()
        WHERE id(relation) in $relationshipIds
        DELETE relation
        """;

    private static final String MATCH_LINK_NODES_TEMPLATE = """ 
        MATCH (n1) WHERE id(n1) = %s
        MATCH (n2) WHERE id(n2) = %s
        """;

    private static final String CREATE_LINK_LEFT_TO_RIGHT_TEMPLATE = """
        CREATE (n1)-[:`%s`]->(n2)
        """;

    private static final String CREATE_LINK_RIGHT_TO_LEFT_TEMPLATE = """
        CREATE (n1)<-[:`%s`]-(n2)
        """;

    // TODO - test saving by passing projection to Neo4jTemplate::save(...)
    public boolean save(final List<RelationshipVo> relationships, final Transaction transaction)
    {
        if (relationships.isEmpty())
        {
            return true;
        }

        final var saveQueries = buildSaveQueries(relationships);
        saveQueries.forEach(transaction::run);
        return true;
    }

    public boolean delete(final List<RelationshipVo> relationships, final Transaction transaction)
    {
        if (relationships.isEmpty())
        {
            return true;
        }

        final var deleteQuery = buildDeleteQuery(relationships);
        transaction.run(deleteQuery);
        return true;
    }

    private List<Query> buildSaveQueries(final List<RelationshipVo> relationships)
    {
        final var relationshipsGroupedByLink = relationships.stream()
            .collect(Collectors.groupingBy(x -> RelationshipId.builder()
                .withStartNode(Math.min(x.getSourceId(), x.getDestinationId()))
                .withEndNode(Math.max(x.getSourceId(), x.getDestinationId()))
                .build()));

        return relationshipsGroupedByLink.entrySet().stream()
            .map(relationshipsByLink -> buildRelationshipQueryForLink(relationshipsByLink.getKey(), relationshipsByLink.getValue()))
            .toList();
    }

    private Query buildRelationshipQueryForLink(final RelationshipId relationshipId, List<RelationshipVo> relationships)
    {
        final var start = relationshipId.startNode();
        final var end = relationshipId.endNode();

        final var query = new StringBuilder(MATCH_LINK_NODES_TEMPLATE.formatted(start, end));

        relationships.forEach(relationship -> {
            final var outgoing = relationship.getSourceId() == start && relationship.getDestinationId() == end;

            final var createRelationshipStatement = outgoing ?
                CREATE_LINK_LEFT_TO_RIGHT_TEMPLATE.formatted(relationship.getRelationship()) :
                CREATE_LINK_RIGHT_TO_LEFT_TEMPLATE.formatted(relationship.getRelationship());

            query.append("\n").append(createRelationshipStatement);
        });

        return new Query(query.toString());
    }

    private Query buildDeleteQuery(final List<RelationshipVo> relationships)
    {
        final var relationshipIds = relationships.stream()
            .map(RelationshipVo::getRelationshipId)
            .distinct()
            .toList();

        return new Query(DELETE_RELATIONSHIPS_BY_ID, Map.of(RELATIONSHIP_IDS_PARAMETER, relationshipIds));
    }
}
