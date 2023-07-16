package com.rdfsonto.elastic.service;

public interface ElasticSearchClassNodeBulkService
{
    void createIndex(long userId, long projectId);

    void deleteIndex(long userId, long projectId);
}
