package com.rdfsonto.elastic.service;

import static com.rdfsonto.elastic.service.ElasticSearchClassNodeServiceImpl.NEO4J_ID_FIELD;
import static com.rdfsonto.elastic.service.ElasticSearchClassNodeServiceImpl.NEO4J_URI_FIELD;

import java.util.Map;

import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.core.search.Hit;


@Component
class ElasticSearchClassNodeMapper
{
    ElasticSearchClassNode map(final Hit<Map> hit)
    {
        if (hit.source() == null)
        {
            return null;
        }

        final var uri = hit.source().get(NEO4J_URI_FIELD).toString();
        final var id = Long.parseLong(hit.source().get(NEO4J_ID_FIELD).toString());

        return new ElasticSearchClassNode(id, uri);
    }
}
