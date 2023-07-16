package com.rdfsonto.elastic.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.FilterCondition;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticSearchClassNodeServiceImpl implements ElasticSearchClassNodeService
{
    private static final String STANDARD_QUERY_ANALYZER = "standard";
    private static final String LABELS_QUERY_ANALYZER = "my_analyzer";
    private static final String NEO4J_URI_FIELD = "neo4j_uri";
    private static final String NEO4J_ID_FIELD = "neo4j_id";
    private static final String NEO4J_LABELS_FIELD = "neo4j_labels";
    private static final String INDEX_PREFIX_TEMPLATE = "ontology-index-%s-%s";
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticSearchClassNodeMapper mapper;

    @Override
    public List<ElasticSearchClassNode> search(final long userId,
                                               final long projectId,
                                               final List<FilterCondition> filters,
                                               final List<String> labels,
                                               final Pageable pageable)
    {
        final var propertyQuery = filters.stream()
            .map(this::filterConditionToQuery)
            .toList();

        final var labelQuery = labels.stream()
            .map(label -> new Query.Builder()
                .match(match -> match.field(NEO4J_LABELS_FIELD).analyzer(LABELS_QUERY_ANALYZER).query(label))
                .build())
            .toList();

        final var queries = Stream.of(labelQuery, propertyQuery)
            .filter(query -> !query.isEmpty())
            .map(query -> new BoolQuery.Builder().must(query).build()._toQuery())
            .toList();

        final var boolQuery = new BoolQuery.Builder().must(queries);

        try
        {
            final var hits = elasticsearchClient.search(search -> search
                    .from((int) pageable.getOffset())
                    .size(pageable.getPageSize())
                    .index(getIndexName(userId, projectId))
                    .query(q -> q.bool(b -> boolQuery)), Map.class)
                .hits().hits();

            return hits.stream()
                .map(mapper::map)
                .toList();
        }
        catch (final IOException exception)
        {
            log.error(exception.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void save(final long userId, final long projectId, final ClassNode classNode)
    {
        final var propertiesMap = extractProperties(classNode);

        try
        {
            elasticsearchClient.index(idx -> idx
                .index(getIndexName(userId, projectId))
                .id(classNode.uri())
                .document(propertiesMap));
        }
        catch (final IOException exception)
        {
            throw new IllegalStateException("Failed to index a node.");
        }
    }

    @Override
    public void delete(final long userId, final long projectId, final ClassNode classNode)
    {
        try
        {
            elasticsearchClient.delete(d -> d
                .index(getIndexName(userId, projectId))
                .id(classNode.uri()));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to delete indexed document.");
        }
    }

    static Map<String, Object> extractProperties(final ClassNode node)
    {
        final var labelsField = Map.entry(NEO4J_LABELS_FIELD, node.classLabels().toString());
        final var uriField = Map.entry(NEO4J_URI_FIELD, node.uri());
        final var idField = Map.entry(NEO4J_ID_FIELD, node.id().toString());

        final var neo4jFields = Stream.of(labelsField, uriField, idField);
        final var properties = node.properties().entrySet().stream()
            .map(property -> Map.entry(simplifyField(property.getKey()), property.getValue().toString()));

        return Stream.concat(properties, neo4jFields)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static String simplifyField(final String field)
    {
        return field
            .replaceFirst("http://", "")
            .replaceFirst("https://", "")
            .replace('.', '_');
    }

    static String getIndexName(final long userId, final long projectId)
    {
        return INDEX_PREFIX_TEMPLATE.formatted(userId, projectId);
    }

    private Query filterConditionToQuery(final FilterCondition filter)
    {

        final var property = filter.property().equals("uri") ? NEO4J_URI_FIELD : simplifyField(filter.property());
        final var value = filter.value();

        return switch (filter.operator())
        {
            case EQUALS -> Query.of(q -> q.matchPhrase(p -> p.field(property).query(value).analyzer(STANDARD_QUERY_ANALYZER)));
            case CONTAINS -> Query.of(q -> q.match(m -> m.field(property).query(value).analyzer(STANDARD_QUERY_ANALYZER)));
            case EXISTS -> Query.of(q -> q.exists(e -> e.field(property)));
            default -> throw new NotImplementedException("Handling of %s is not implemented".formatted(filter.operator()));
        };
    }
}
