package com.rdfsonto.exportonto.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class Neo4jBigGraphSerializer
{
    @Value("${spring.neo4j.url}")
    private String databaseUrl;

    @Value("${neo4j.serializer.credential}")
    private String authCredentials;

    @Value("${rdf4j.downloader.workspace}")
    private String WORKSPACE_DIR;

    private final String SERIALIZATION_ENDPOINT = "/rdf/neo4j/cypher";

    private final String SERIALIZE_GRAPH_TEMPLATE = """
            {
                "cypher" : "MATCH (n:Resource:`%s`) WHERE NOT (n)-[]->() return n , null as r UNION MATCH (n:Resource:`%s`)-[r]->() return n,r",
                "format" : "%s"
            }
        """;

    Path serializeBigGraph(final UUID exportId, final String projectLabel, final RDFFormat rdfFormat) throws IOException, InterruptedException
    {
        final var queryBody = prepareQuery(projectLabel, rdfFormat);

        final var request = HttpRequest.newBuilder()
            .uri(URI.create(databaseUrl + SERIALIZATION_ENDPOINT))
            .setHeader("Authorization", "Basic %s".formatted(authCredentials))
            .POST(HttpRequest.BodyPublishers.ofString(queryBody))
            .build();

        final var response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofInputStream());

        final var serializedFilePath = exportIdToPath(exportId, rdfFormat);

        final var input = response.body();
        final var output = new FileOutputStream(serializedFilePath.toFile());

        input.transferTo(output);

        input.close();
        output.close();

        return serializedFilePath;
    }

    private String prepareQuery(final String projectTag, final RDFFormat rdfFormat)
    {
        return SERIALIZE_GRAPH_TEMPLATE.formatted(projectTag, projectTag, rdfFormat.getName());
    }

    private Path exportIdToPath(final UUID exportId, final RDFFormat rdfFormat)
    {
        final var extension = rdfFormat.getDefaultFileExtension();
        return Path.of(WORKSPACE_DIR + exportId + "." + extension);
    }
}
