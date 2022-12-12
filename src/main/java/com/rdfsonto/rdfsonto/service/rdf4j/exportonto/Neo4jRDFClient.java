package com.rdfsonto.rdfsonto.service.rdf4j.exportonto;

import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class Neo4jRDFClient
{
    private static final String GET_ALL_NODES_OF_PROJECT_URL = "/rdf/neo4j/cypher";
    private static final String GET_ALL_NODES_OF_PROJECT_REQUEST_BODY_TEMPLATE = """
        {
            "cypher" : "MATCH pattern = (n:%s)-[]-() RETURN pattern"
            "format" : "%s"
        }
         """;

    @Value("${spring.neo4j.url}")
    private String neo4jHost;
    private final RestTemplate restTemplate;

    public String serializeNeo4jGraphToRDF(final String projectTag, final RDFFormat rdfFormat)
    {
        final var httpEntity = prepareHttpEntityGetAllNodesInProject(projectTag, rdfFormat);

        return restTemplate.exchange(
            neo4jHost + GET_ALL_NODES_OF_PROJECT_URL,
            HttpMethod.POST,
            httpEntity,
            String.class).getBody();
    }

    public Path serializeBigNeo4jGraphToRDF(final String projectTag, final RDFFormat rdfFormat)
    {
        return null;
    }

    private HttpEntity<?> prepareHttpEntityGetAllNodesInProject(final String projectTag, final RDFFormat rdfFormat)
    {
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final var body = GET_ALL_NODES_OF_PROJECT_REQUEST_BODY_TEMPLATE.formatted(
            projectTag,
            rdfFormat.getName()
        );

        return new HttpEntity<>(body, headers);
    }
}
