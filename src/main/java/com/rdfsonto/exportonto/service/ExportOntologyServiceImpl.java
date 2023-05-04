package com.rdfsonto.exportonto.service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.service.UniqueUriIdHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
class ExportOntologyServiceImpl implements ExportOntologyService
{
    private static final long MAX_NODES_CHUNK = 1000;
    private static final String PROJECT_LABEL_ROOT = "https://www.user_neo4j.com#";

    @Value("${rdf4j.downloader.workspace}")
    private String WORKSPACE_DIR;
    private final ClassNodeRepository classNodeRepository;
    private final Neo4jGraphSerializer neo4JGraphSerializer;
    private final RDFExporter rdfExporter;
    private final UniqueUriIdHandler uniqueUriIdHandler;

    @Override
    public ExtractedOntology extractOntology(final Long userId, final Long projectId, final RDFFormat rdfFormat)
    {
        final var ontologyTag = uniqueUriIdHandler.uniqueUri(userId, projectId);
        final var projectLabel = PROJECT_LABEL_ROOT + ontologyTag;
        final var projectNodesCount = classNodeRepository.countAllByClassLabelsContaining(projectLabel);

        final var extractedOntologyBuilder = ExtractedOntology.builder()
            .withRdfFormat(rdfFormat);

        if (projectNodesCount <= MAX_NODES_CHUNK)
        {
            final var serializedRDFGraph = neo4JGraphSerializer.serializeNeo4jGraphToRDF(projectLabel, rdfFormat);
            final var savedFile = saveToFile(serializedRDFGraph, ontologyTag);

            extractedOntologyBuilder.withPath(savedFile);
        }
        else
        {
            // TODO take care of bigger files
            // TODO: https://stackoverflow.com/questions/57289877/how-to-paginate-results-of-cypher-neo4j
            final var savedFile = neo4JGraphSerializer.serializeBigNeo4jGraphToRDF(ontologyTag, rdfFormat);
            extractedOntologyBuilder.withPath(savedFile);
        }

        return extractedOntologyBuilder.build();
    }

    @Override
    public ExportOntologyResult exportOntology(final Long userId, final Long projectId, final ExtractedOntology extractedOntology)
    {
        final var ontologyTag = uniqueUriIdHandler.uniqueUri(userId, projectId);

        try
        {
            rdfExporter.prepareRDFFileForExport(extractedOntology.path(), ontologyTag, extractedOntology.rdfFormat());

            final var inputStream = new BufferedInputStream(new FileInputStream(extractedOntology.path().toFile()));
            final var file = extractedOntology.path().toFile();

            return ExportOntologyResult.builder()
                .withOntologyFile(file)
                .withInputStream(inputStream)
                .build();
        }
        catch (final IOException ioException)
        {
            return ExportOntologyResult.builder()
                .withIoException(ioException)
                .build();
        }
    }

    private Path saveToFile(final String serializedGraph, final String ontologyTag)
    {
        final var filePath = Path.of(WORKSPACE_DIR + ontologyTag + "_graph.rdf");

        try
        {
            final var fileWriter = new FileWriter(filePath.toFile().getAbsolutePath());
            fileWriter.write(serializedGraph);
            fileWriter.close();

            return filePath;
        }
        catch (final IOException e)
        {
            log.error("Failed to save to file: {}", filePath);
            return null;
        }
    }
}