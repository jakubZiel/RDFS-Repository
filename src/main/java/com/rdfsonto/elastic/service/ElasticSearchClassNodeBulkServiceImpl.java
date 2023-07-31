package com.rdfsonto.elastic.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.service.ClassNodeService;
import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.classnode.service.UriUniquenessHandler;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchClassNodeBulkServiceImpl implements ElasticSearchClassNodeBulkService
{
    private static final int BATCH_SIZE = 50_000;
    private static final int MAX_REQUEST_SIZE_MB = 50_000_000;
    private static final int FLUSH_INTERVAL_SECONDS = 10;

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchAsyncClient elasticsearchAsyncClient;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final UriUniquenessHandler uriUniquenessHandler;
    private final UniqueUriIdHandler uniqueUriIdHandler;
    private final ClassNodeService classNodeService;

    @Override
    public void createIndex(final long userId, final long projectId)
    {
        final var projectTag = uniqueUriIdHandler.uniqueUri(userId, projectId);
        final var projectLabel = List.of(uriUniquenessHandler.getClassNodeLabel(projectTag));

        final var nodeCount = classNodeNeo4jDriverRepository.countNodeIdsByPropertiesAndLabels(projectLabel, List.of());
        final int pageCount = (int) (nodeCount / BATCH_SIZE) + 1;

        final BulkIngester<Void> bulkIngester = BulkIngester.of(ingester -> ingester
            .client(elasticsearchClient)
            .maxSize(MAX_REQUEST_SIZE_MB)
            .flushInterval(FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS));

        IntStream.range(0, pageCount).forEach(batchIndex -> handleBulkIndex(batchIndex, userId, projectId, bulkIngester));

        elasticsearchAsyncClient.indices()
            .refresh(refresh -> refresh.index(ElasticSearchClassNodeServiceImpl.getIndexName(userId, projectId)));

        bulkIngester.close();
    }

    private void handleBulkIndex(final int batchIndex,
                                 final long userId,
                                 final long projectId,
                                 final BulkIngester<Void> bulkIngester)
    {
        final var page = Pageable.ofSize(BATCH_SIZE).withPage(batchIndex);
        final var fetchedNodes = classNodeService.findByProject(projectId, page);
        fetchedNodes.forEach(fetchedNode -> {
            final var propertiesMap = ElasticSearchClassNodeServiceImpl.extractProperties(fetchedNode);
            bulkIngester.add(op -> op
                .index(idx -> idx
                    .index(ElasticSearchClassNodeServiceImpl.getIndexName(userId, projectId))
                    .id(fetchedNode.uri())
                    .document(propertiesMap)));
        });
    }
}
