package com.rdfsonto.exportonto.service;

import static org.eclipse.rdf4j.rio.RDFFormat.RDFXML;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Component;

import com.rdfsonto.exportonto.rest.GraphSerializeRequest;
import com.rdfsonto.infrastructure.feign.Neo4jRdfClient;

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

    public static void main(String[] args)
    {
        Path source = Paths.get("/home/jzielins/5521acb6-17a6-481a-ab0c-53c8b4c151c9.ttl");
        Path target = Paths.get("/home/jzielins/5521acb6-17a6-481a-ab0c-53c8b4c151c9.ttl.gz");

        if (Files.notExists(source)) {
            System.err.printf("The path %s doesn't exist!", source);
            return;
        }

        try {

            compressGzip(source, target);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // copy file (FileInputStream) to GZIPOutputStream
    public static void compressGzip(Path source, Path target) throws IOException {

        try (GZIPOutputStream gos = new GZIPOutputStream(
            new FileOutputStream(target.toFile()));
             FileInputStream fis = new FileInputStream(source.toFile())) {

            // copy file
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gos.write(buffer, 0, len);
            }
        }

    }

}
