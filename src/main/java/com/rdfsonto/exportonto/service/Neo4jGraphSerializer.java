package com.rdfsonto.exportonto.service;

import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Component;

import com.rdfsonto.exportonto.rest.GraphSerializeRequest;
import com.rdfsonto.exportonto.rest.Neo4jRdfClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class Neo4jGraphSerializer
{
    private static final String GET_PROJECT_NODES_QUERY_TEMPLATE = "MATCH pattern = (n:`%s`)-[]-() RETURN pattern";

    private final Neo4jRdfClient client;

    public String serializeNeo4jGraphToRDF(final String projectTag, final RDFFormat rdfFormat)
    {
        final var request = GraphSerializeRequest.builder()
            .withCypher(GET_PROJECT_NODES_QUERY_TEMPLATE.formatted(projectTag))
            .withFormat(rdfFormat.getName())
            .build();

        return client.serializeGraphToRdf(request);
    }

    public Path serializeBigNeo4jGraphToRDF(final String projectTag, final RDFFormat rdfFormat)
    {
        return null;
    }
}
