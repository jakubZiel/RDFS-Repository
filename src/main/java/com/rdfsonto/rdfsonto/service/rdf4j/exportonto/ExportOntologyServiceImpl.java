package com.rdfsonto.rdfsonto.service.rdf4j.exportonto;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
class ExportOntologyServiceImpl implements ExportOntologyService
{
    private static final long MAX_NODES_CHUNK = 20_000;

    @Value("${rdf4j.downloader.workspace}")
    private String WORKSPACE_DIR;
    private final ClassNodeRepository classNodeRepository;
    private final Neo4jRDFClient neo4jRDFClient;

    @Override
    public ExtractedOntology extractOntology(final Long userId, final String projectName, final RDFFormat rdfFormat)
    {
        final var ontologyTag = ontologyTag(userId, projectName);
        final var projectNodesCount = classNodeRepository.countAllByClassLabelsContaining(ontologyTag);

        final var extractedOntologyBuilder = ExtractedOntology.builder()
            .withRdfFormat(rdfFormat);

        if (projectNodesCount <= MAX_NODES_CHUNK)
        {
            final var serializedRDFGraph = neo4jRDFClient.serializeNeo4jGraphToRDF(ontologyTag, rdfFormat);
            final var savedFile = saveToFile(serializedRDFGraph, ontologyTag);

            extractedOntologyBuilder.withPath(savedFile);
        }
        else
        {
            // TODO take care of bigger files
            // TODO: https://stackoverflow.com/questions/57289877/how-to-paginate-results-of-cypher-neo4j
            final var savedFile = neo4jRDFClient.serializeBigNeo4jGraphToRDF(ontologyTag, rdfFormat);
            extractedOntologyBuilder.withPath(savedFile);
        }

        return extractedOntologyBuilder.build();
    }

    @Override
    public ExportOntologyResult exportOntology(final Long userId, final String projectName, final ExtractedOntology extractedOntology)
    {
        final var ontologyTag = ontologyTag(userId, projectName);
        final var rdfExporter = new RDFExporter(extractedOntology.rdfFormat());

        try
        {
            rdfExporter.prepareRDFFileForExport(extractedOntology.path(), ontologyTag);

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

    @Override
    public void clearWorkspace(final Path filePath)
    {
        try
        {
            Files.deleteIfExists(filePath);
        }
        catch (IOException e)
        {
            log.error("Failed to delete file: {}", filePath);
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

    private String ontologyTag(final Long userId, final String projectName)
    {
        return userId.toString() + projectName;
    }
}
