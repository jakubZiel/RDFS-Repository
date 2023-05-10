package com.rdfsonto.exportonto.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PROJECT_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_USER_ID;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.prefix.service.PrefixMapping;
import com.rdfsonto.prefix.service.PrefixNodeService;
import com.rdfsonto.project.service.ProjectService;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
class ExportOntologyServiceImpl implements ExportOntologyService
{
    private static final long MAX_NODES_CHUNK = 1000;
    private static final String PROJECT_LABEL_ROOT = "http://www.user_neo4j.com#";

    @Value("${rdf4j.downloader.workspace}")
    private String WORKSPACE_DIR;
    private final ProjectService projectService;
    private final UserService userService;
    private final PrefixNodeService prefixNodeService;
    private final ClassNodeRepository classNodeRepository;
    private final Neo4jGraphSerializer neo4JGraphSerializer;
    private final RDFExporter rdfExporter;
    private final UniqueUriIdHandler uniqueUriIdHandler;

    public ExportOntologyResult exportOntology(final long userId, final long projectId, final RDFFormat rdfFormat)
    {
        projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException(
                "Project with ID: %s does not exist, could not export.".formatted(projectId),
                INVALID_PROJECT_ID));

        userService.findById(userId)
            .orElseThrow(() -> new ClassNodeException(
                "Attempted ontology export for non-existing user ID: $%s.".formatted(userId),
                INVALID_USER_ID));

        try
        {
            final var extractedOntology = extractOntology(userId, projectId, rdfFormat);
            return exportOntology(userId, projectId, extractedOntology);
        }
        catch (final IOException exception)
        {
            log.error(exception.getMessage());
            throw new IllegalStateException("Failed to extract ontology to a file.");
        }
    }

    private ExtractedOntology extractOntology(final Long userId, final Long projectId, final RDFFormat rdfFormat) throws IOException
    {
        final var ontologyTag = uniqueUriIdHandler.uniqueUri(userId, projectId);
        final var projectLabel = PROJECT_LABEL_ROOT + ontologyTag;
        final var projectNodesCount = classNodeRepository.countAllByClassLabelsContaining(projectLabel);

        final var extractedOntologyBuilder = ExtractedOntology.builder()
            .withRdfFormat(rdfFormat);

        final var exportId = UUID.randomUUID();

        if (projectNodesCount <= MAX_NODES_CHUNK)
        {
            final var serializedRDFGraph = neo4JGraphSerializer.serializeNeo4jGraphToRDF(projectLabel);
            saveToFile(exportId, serializedRDFGraph, rdfFormat);

            extractedOntologyBuilder.withExportId(exportId);
        }
        else
        {
            // TODO take care of bigger files
            // TODO: https://stackoverflow.com/questions/57289877/how-to-paginate-results-of-cypher-neo4j
            neo4JGraphSerializer.serializeBigNeo4jGraphToRDF(ontologyTag, rdfFormat);
            extractedOntologyBuilder.withExportId(exportId);
        }

        return extractedOntologyBuilder.build();
    }

    private ExportOntologyResult exportOntology(final Long userId, final Long projectId, final ExtractedOntology extractedOntology)
    {
        final var ontologyTag = uniqueUriIdHandler.uniqueUri(userId, projectId);
        final var exportId = extractedOntology.exportId();
        final var rdfFormat = extractedOntology.rdfFormat();

        try
        {
            final var extractedOntologyFile = new File(exportIdToPath(exportId, rdfFormat).toString());
            final var absolutePath = extractedOntologyFile.getCanonicalPath();
            final var namespaces = prefixNodeService.findAll(projectId).map(PrefixMapping::prefixToUri).orElse(Map.of());

            final var exportedOntologyFile =
                rdfExporter.prepareRdfFileForExport(Path.of(absolutePath), ontologyTag, rdfFormat, namespaces);

            final var inputStream = new BufferedInputStream(new FileInputStream(exportedOntologyFile));

            return ExportOntologyResult.builder()
                .withExportId(exportId)
                .withInputStream(inputStream)
                .withExportedOntologyFile(exportedOntologyFile)
                .build();
        }
        catch (final IOException ioException)
        {
            throw new IllegalStateException(ioException.getMessage());
        }
    }

    private void saveToFile(final UUID exportId, final String serializedGraph, final RDFFormat rdfFormat) throws IOException
    {
        final var filePath = exportIdToPath(exportId, rdfFormat);

        final var fileWriter = new FileWriter(filePath.toFile().getAbsolutePath());
        fileWriter.write(serializedGraph);
        fileWriter.close();
    }

    private Path exportIdToPath(final UUID exportId, final RDFFormat rdfFormat)
    {
        final var extension = rdfFormat.getDefaultFileExtension();
        return Path.of(WORKSPACE_DIR + exportId + "." + extension);
    }
}