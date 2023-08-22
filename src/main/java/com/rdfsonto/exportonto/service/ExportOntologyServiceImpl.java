package com.rdfsonto.exportonto.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PROJECT_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_USER_ID;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.ClassNodeService;
import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.classnode.service.UriRemoveUniquenessHandler;
import com.rdfsonto.classnode.service.UriUniquenessHandler;
import com.rdfsonto.exportonto.database.ExportRepository;
import com.rdfsonto.infrastructure.workspacemanagement.WorkspaceManagementService;
import com.rdfsonto.prefix.service.PrefixNodeService;
import com.rdfsonto.project.database.ProjectRepository;
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
    private final int BATCH_SIZE = 100_000;
    private static final long MAX_NODE_COUNT = 1000;
    private static final String PROJECT_LABEL_ROOT = "http://www.user_neo4j.com#";

    @Value("${rdf4j.downloader.workspace}")
    private String WORKSPACE_DIR;
    private final ProjectService projectService;
    private final UserService userService;
    private final PrefixNodeService prefixNodeService;
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final Neo4jGraphSerializer neo4JGraphSerializer;
    private final RDFExporter rdfExporter;
    private final UniqueUriIdHandler uniqueUriIdHandler;
    private final ExportRepository exportRepository;
    private final ClassNodeService classNodeService;
    private final UriUniquenessHandler uriUniquenessHandler;
    private final UriRemoveUniquenessHandler uriRemoveUniquenessHandler;
    private final ProjectRepository projectRepository;
    private final WorkspaceManagementService workspaceManagementService;
    private final Neo4jBigGraphSerializer neo4jBigGraphSerializer;

    @Override
    public ExportOntologyResult exportOntology(final long userId, final long projectId, final RDFFormat rdfFormat)
    {
        final var project = projectService.findById(projectId)
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
            final var exportedOntology = exportOntology(userId, projectId, extractedOntology);

            final var snapshotName = exportedOntology.exportedOntologyFile().getName();
            final var snapshotTime = System.currentTimeMillis();

            projectRepository.saveSnapshot(projectId, snapshotTime, snapshotName);

            final var oldSnapshotFile = project.getSnapshotFile();
            workspaceManagementService.clearWorkspace(oldSnapshotFile);

            return exportedOntology;
        }
        catch (final IOException | InterruptedException exception)
        {
            log.error(exception.getMessage());
            throw new IllegalStateException("Failed to extract ontology to a file.");
        }
    }

    @Override
    public SnapshotExport provideExportedSnapshot(final long userId, final long projectId)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException(
                "Project with ID: %s does not exist, could not export.".formatted(projectId),
                INVALID_PROJECT_ID));

        userService.findById(userId)
            .orElseThrow(() -> new ClassNodeException(
                "Attempted ontology export for non-existing user ID: $%s.".formatted(userId),
                INVALID_USER_ID));

        final var snapshotPath = project.getSnapshotFile() != null ? Path.of(WORKSPACE_DIR + project.getSnapshotFile()) : null;

        if (snapshotPath == null || !Files.exists(snapshotPath))
        {
            return SnapshotExport.builder()
                .withFileInputStream(InputStream.nullInputStream())
                .build();
        }
        try
        {
            return SnapshotExport.builder()
                .withSnapshotTime(project.getSnapshotTime())
                .withFileName(project.getProjectName())
                .withFileInputStream(new BufferedInputStream(new FileInputStream(snapshotPath.toFile())))
                .build();
        }
        catch (FileNotFoundException e)
        {
            throw new IllegalStateException("Failed to open input stream to file %s".formatted(snapshotPath));
        }
    }

    private ExtractedOntology extractOntology(final Long userId, final Long projectId, final RDFFormat rdfFormat)
        throws IOException, InterruptedException
    {
        final var ontologyTag = uniqueUriIdHandler.uniquerUriTag(userId, projectId);
        final var projectLabel = PROJECT_LABEL_ROOT + ontologyTag;
        final var projectNodesCount = classNodeNeo4jDriverRepository.countNodeIdsByPropertiesAndLabels(List.of(projectLabel), List.of());

        final var extractedOntologyBuilder = ExtractedOntology.builder()
            .withRdfFormat(rdfFormat);

        final var exportId = UUID.randomUUID();

        if (projectNodesCount <= MAX_NODE_COUNT)
        {
            final var serializedRDFGraph = neo4JGraphSerializer.serializeNeo4jGraphToRDF(projectLabel);
            saveToFile(exportId, serializedRDFGraph, rdfFormat);
            extractedOntologyBuilder.withExportId(exportId);
            extractedOntologyBuilder.withAlreadyUntagged(false);
        }
        else
        {
            // TODO take care of bigger files
            // TODO: https://stackoverflow.com/questions/57289877/how-to-paginate-results-of-cypher-neo4j
            /*saveToBigFile(exportId, projectId, userId, rdfFormat);
            extractedOntologyBuilder.withExportId(exportId);
            extractedOntologyBuilder.withAlreadyUntagged(true);*/

            extractToBigFile(exportId, projectId, userId, rdfFormat);
            final var exportedFile = exportToBigFile(exportId, rdfFormat);
            extractedOntologyBuilder.withExportId(exportId);
            extractedOntologyBuilder.withAlreadyUntagged(true);
            extractedOntologyBuilder.withExtractedFile(exportedFile.toFile());
        }

        return extractedOntologyBuilder.build();
    }

    private ExportOntologyResult exportOntology(final Long userId, final Long projectId, final ExtractedOntology extractedOntology)
    {
        final var ontologyTag = uniqueUriIdHandler.uniquerUriTag(userId, projectId);
        final var exportId = extractedOntology.exportId();
        final var rdfFormat = extractedOntology.rdfFormat();

        try
        {
            final var extractedOntologyFile = new File(exportIdToExportedPath(exportId, rdfFormat).toString());
            final var absolutePath = extractedOntologyFile.getCanonicalPath();

            // TODO
            if (extractedOntology.alreadyUntagged())
            {
                final var extractedFile = extractedOntology.extractedFile();
                final var compressedFile = Paths.get(exportIdToPath(exportId, rdfFormat) + ".gz");
                compressGzip(extractedFile.toPath(), compressedFile);

                workspaceManagementService.clearWorkspace(extractedFile.getName());

                return ExportOntologyResult.builder()
                    .withExportId(exportId)
                    .withExportedOntologyFile(compressedFile.toFile())
                    .withInputStream(null)
                    .build();
            }

            // TODO
            // final var namespaces = prefixNodeService.findAll(projectId).map(PrefixMapping::prefixToUri).orElse(Map.of());
            final var namespaces = Map.of(
                "dc", "http://purl.org/dc/elements/1.1/",
                "obo", "http://purl.obolibrary.org/obo/",
                "owl", "http://www.w3.org/2002/07/owl#",
                "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                "doid", "http://purl.obolibrary.org/obo/doid#",
                "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
                "skos", "http://www.w3.org/2004/02/skos/core#",
                "terms", "http://purl.org/dc/terms/",
                "oboInOwl", "http://www.geneontology.org/formats/oboInOwl#"
            );
            final var exportedOntologyFile = rdfExporter.prepareRdfFileForExport(Path.of(absolutePath), ontologyTag, rdfFormat, namespaces);

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

    private void saveToBigFile(final UUID exportId, final long projectId, final long userId, final RDFFormat rdfFormat) throws IOException
    {
        final var exportFile = exportIdToPath(exportId, rdfFormat).toFile();

        final var projectTag = uniqueUriIdHandler.uniquerUriTag(userId, projectId);
        final var projectLabel = List.of(uriUniquenessHandler.getClassNodeLabel(projectTag));

        final var nodeCount = classNodeNeo4jDriverRepository.countNodeIdsByPropertiesAndLabels(projectLabel, List.of());
        final int pageCount = (int) (nodeCount / BATCH_SIZE) + 1;

        final var outputStream = new BufferedOutputStream(new FileOutputStream(exportFile));
        final var rdfWriter = Rio.createWriter(rdfFormat, outputStream);
        final var streamExportHandler = new RDFStreamExportHandler(rdfWriter, uriRemoveUniquenessHandler);
        final var counter = new ArrayList<>(List.of(0));

        streamExportHandler.startRDF();
        IntStream.range(0, pageCount).forEach(batchIndex -> {
            final var page = Pageable.ofSize(BATCH_SIZE).withPage(batchIndex);
            final var statements = exportRepository.exportAttributes(projectId, userId, page);

            log.info("written statements : {}", statements.size());
            statements.forEach(streamExportHandler::handleStatement);
            counter.set(0, counter.get(0) + statements.size());
        });
        log.info("exported : " + counter.get(0));
/*
        IntStream.range(0, pageCount).forEach(batchIndex -> {
            final var page = Pageable.ofSize(BATCH_SIZE).withPage(batchIndex);
            final var statements = exportRepository.exportRelationships(projectId, userId, page);

            log.info("written statements : {}", statements.size());
            statements.forEach(streamExportHandler::handleStatement);
            counter.set(0, counter.get(0) + statements.size());
        });
        log.info("exported : " + counter.get(0));*/

        streamExportHandler.endRDF();
        outputStream.close();
    }

    private Path extractToBigFile(final UUID exportId, final long projectId, final long userId, final RDFFormat rdfFormat)
        throws IOException, InterruptedException
    {
        final var projectTag = uniqueUriIdHandler.uniquerUriTag(userId, projectId);
        final var projectLabel = uriUniquenessHandler.getClassNodeLabel(projectTag);

        return neo4jBigGraphSerializer.serializeBigGraph(exportId, projectLabel, rdfFormat);
    }

    private Path exportToBigFile(final UUID exportId, final RDFFormat rdfFormat) throws IOException
    {
        final var extractedFile = exportIdToPath(exportId, rdfFormat);
        final var outputFile = exportIdToExportedPath(exportId, rdfFormat);

        final var input = new FileInputStream(extractedFile.toFile());
        final var output = new FileOutputStream(outputFile.toFile());

        final var writer = Rio.createWriter(rdfFormat, output);
        final var exportHandler = new RDFStreamExportHandler(writer, uriRemoveUniquenessHandler);

        final var parser = Rio.createParser(rdfFormat);

        parser.setRDFHandler(exportHandler);
        parser.parse(input);

        workspaceManagementService.clearWorkspace(extractedFile.getFileName().toString());

        return outputFile;
    }

    public static void compressGzip(Path source, Path target) throws IOException
    {

        try (GZIPOutputStream gos = new GZIPOutputStream(
            new FileOutputStream(target.toFile()));
             FileInputStream fis = new FileInputStream(source.toFile()))
        {

            // copy file
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0)
            {
                gos.write(buffer, 0, len);
            }
        }
    }

    private Path exportIdToPath(final UUID exportId, final RDFFormat rdfFormat)
    {
        final var extension = rdfFormat.getDefaultFileExtension();
        return Path.of(WORKSPACE_DIR + exportId + "." + extension);
    }

    private Path exportIdToExportedPath(final UUID exportId, final RDFFormat rdfFormat)
    {
        return Path.of(exportIdToPath(exportId, rdfFormat) + ".out");
    }
}