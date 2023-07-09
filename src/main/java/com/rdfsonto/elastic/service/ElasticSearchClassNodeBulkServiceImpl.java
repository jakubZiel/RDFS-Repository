package com.rdfsonto.elastic.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.service.ClassNodeService;
import com.rdfsonto.classnode.service.UriUniquenessHandler;
import com.rdfsonto.elastic.model.IndexingResult;
import com.rdfsonto.project.database.ProjectNode;
import com.rdfsonto.project.service.ProjectService;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class ElasticSearchClassNodeBulkServiceImpl implements ElasticSearchClassNodeBulkService
{
    private static final int BATCH_SIZE = 50_000;
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchAsyncClient elasticsearchAsyncClient;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ProjectService projectService;
    private final UriUniquenessHandler uriUniquenessHandler;
    private final ClassNodeService classNodeService;

    @Override
    public IndexingResult createIndex(final long userId, final long projectId)
    {
        final var projectTag = projectService.findById(projectId)
            .map(projectService::getProjectTag)
            .orElseThrow(() -> new IllegalStateException("Can not index non existing project."));

        final var projectLabel = List.of(uriUniquenessHandler.getClassNodeLabel(projectTag));

        final var nodeCount = classNodeNeo4jDriverRepository.countNodeIdsByPropertiesAndLabels(projectLabel, List.of());
        final int pageCount = (int) (nodeCount / BATCH_SIZE) + 1;

        IntStream.range(0, pageCount).forEach(batchIndex -> handleBulkIndex(batchIndex, userId, projectId, projectLabel));

        elasticsearchAsyncClient.indices()
            .refresh(refresh -> refresh.index(ElasticSearchClassNodeServiceImpl.getIndexName(userId, projectId)));

        return IndexingResult.builder()
            .withSuccess(true)
            .build();
    }

    @Override
    public void deleteIndex(final long userId, final long projectId)
    {
        final var isDeleted = projectService.findById(projectId).isEmpty();

        if (!isDeleted)
        {
            throw new IllegalStateException("Can not delete index of an existing project.");
        }

        final var deletedProject = ProjectNode.builder()
            .withId(projectId)
            .withOwnerId(userId)
            .build();

        final var projectTag = projectService.getProjectTag(deletedProject);

        try
        {
            elasticsearchClient.indices().delete(d -> d.index(projectTag));
        }
        catch (final IOException exception)
        {
            throw new IllegalStateException("Failed to delete index %s".formatted(projectTag));
        }
    }

    private void handleBulkIndex(final int batchIndex,
                                 final long userId,
                                 final long projectId,
                                 final List<String> singleLabel)
    {
        final var page = Pageable.ofSize(BATCH_SIZE).withPage(batchIndex);
        final var fetchedNodes = classNodeService.findByProject(projectId, page);

        final var bulkRequestBuilder = new BulkRequest.Builder();

        fetchedNodes.forEach(fetchedNode -> {
            final var propertiesMap = ElasticSearchClassNodeServiceImpl.extractProperties(fetchedNode);

            bulkRequestBuilder.operations(op -> op
                .index(idx -> idx
                    .index(ElasticSearchClassNodeServiceImpl.getIndexName(userId, projectId))
                    .id(fetchedNode.uri())
                    .document(propertiesMap)));
        });

        try
        {
            elasticsearchClient.bulk(bulkRequestBuilder.build());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
