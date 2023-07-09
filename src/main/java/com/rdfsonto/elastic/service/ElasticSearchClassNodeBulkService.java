package com.rdfsonto.elastic.service;

import com.rdfsonto.elastic.model.IndexingResult;


public interface ElasticSearchClassNodeBulkService
{
    IndexingResult createIndex(long userId, long projectId);

    void deleteIndex(long userId, long projectId);
}
