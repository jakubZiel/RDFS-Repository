package com.rdfsonto.classnode.database;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class ClassNodeNeo4jDriverRepository
{
    private static final String NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)-[rel]->(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, id(n) as source, type(rel) as relation
        """;

    private static final String SAVE_INCOMING_RELATIONSHIPS_QUERY_TEMPLATE = """
        UNWIND %s as incoming
        MATCH (n:Resource), (m:Resource)
        WHERE id(n) = %d and id(m) = incoming[0]
        MERGE (m)-[relation:`incoming[1]`]->(n)
        RETURN relation
        """;

    private static final String SAVE_OUTGOING_RELATIONSHIPS_QUERY_TEMPLATE = """
        UNWIND $outgoing as $outgoing
        MATCH (n:Resource), (m:Resource)
        WHERE id(n) = $nodeId and id(m) = $outgoing[0]
        MERGE (m)<-[relation:`$outgoing[1]`]-(n)
        RETURN relation
        """;

    private static final String NEIGHBOUR_RECORD_KEY = "neighbour";
    private static final String RELATION_RECORD_KEY = "relation";
    private static final String SOURCE_NODE_ID_RECORD_KEY = "source";

    private final Driver driver;
    private final ClassNodeVoMapper classNodeVoMapper;

    public List<ClassNodeVo> findAllOutgoingNeighbours(final List<Long> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return List.of();
        }

        try (final var session = driver.session())
        {
            final var paramMap = Map.of("nodeIds", (Object) ids);
            final var queryResult = session.run(NEIGHBOURS_QUERY_TEMPLATE, paramMap);

            return queryResult.list(record -> classNodeVoMapper.mapToVo(
                record.get(NEIGHBOUR_RECORD_KEY).asNode(),
                record.get(RELATION_RECORD_KEY).asString(),
                record.get(SOURCE_NODE_ID_RECORD_KEY).asLong()));
        }
        catch (final Exception exception)
        {
            log.error("Failed to fetch for outgoing neighbours for nodes with ids: {}, {}", ids, exception.getMessage());
            return null;
        }
    }
}