package com.rdfsonto.exportonto.service;

import static org.eclipse.rdf4j.rio.RDFFormat.RDFXML;
import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

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

    public String serializeNeo4jGraphToRDF(final String projectTag)
    {
        final var request = GraphSerializeRequest.builder()
            .withCypher(GET_PROJECT_NODES_QUERY_TEMPLATE.formatted(projectTag))
            .withFormat(RDFXML.getName())
            .build();

        return client.serializeGraphToRdf(request);
    }

    public void serializeBigNeo4jGraphToRDF(final String projectTag, final RDFFormat rdfFormat)
    {
    }
}
