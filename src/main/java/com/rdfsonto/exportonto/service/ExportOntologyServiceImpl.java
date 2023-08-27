package com.rdfsonto.exportonto.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PROJECT_ID;
import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_USER_ID;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.classnode.service.UriRemoveUniquenessHandler;
import com.rdfsonto.classnode.service.UriUniquenessHandler;
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
    @Value("${rdf4j.downloader.workspace}")
    private String WORKSPACE_DIR;
    private final ProjectService projectService;
    private final UserService userService;
    private final UniqueUriIdHandler uniqueUriIdHandler;
    private final UriUniquenessHandler uriUniquenessHandler;
    private final UriRemoveUniquenessHandler uriRemoveUniquenessHandler;
    private final ProjectRepository projectRepository;
    private final WorkspaceManagementService workspaceManagementService;
    private final Neo4jBigGraphSerializer neo4jBigGraphSerializer;
    private final PrefixNodeService prefixNodeService;

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
            final var compressedFile = prepareCompressedFile(extractedOntology);

            final var snapshotName = compressedFile.exportedOntologyFile().getName();
            final var snapshotTime = System.currentTimeMillis();

            projectRepository.saveSnapshot(projectId, snapshotTime, snapshotName);

            final var oldSnapshotFile = project.getSnapshotFile();
            workspaceManagementService.clearWorkspace(oldSnapshotFile);

            return compressedFile;
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
        final var extractedOntologyBuilder = ExtractedOntology.builder()
            .withRdfFormat(rdfFormat);

        final var exportId = UUID.randomUUID();

        final var extractedFile = extractToBigFile(exportId, projectId, userId, rdfFormat);
        final var exportedFile = exportToBigFile(extractedFile, exportId, projectId, rdfFormat);

        extractedOntologyBuilder.withExportId(exportId);
        extractedOntologyBuilder.withExtractedFile(exportedFile.toFile());

        return extractedOntologyBuilder.build();
    }

    private ExportOntologyResult prepareCompressedFile(final ExtractedOntology extractedOntology)
    {
        final var exportId = extractedOntology.exportId();
        final var rdfFormat = extractedOntology.rdfFormat();

        try
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
        catch (final IOException ioException)
        {
            throw new IllegalStateException(ioException.getMessage());
        }
    }

    private Path extractToBigFile(final UUID exportId, final long projectId, final long userId, final RDFFormat rdfFormat)
        throws IOException, InterruptedException
    {
        final var projectTag = uniqueUriIdHandler.uniquerUriTag(userId, projectId);
        final var projectLabel = uriUniquenessHandler.getClassNodeLabel(projectTag);

        return neo4jBigGraphSerializer.serializeBigGraph(exportId, projectLabel, rdfFormat);
    }

    private Path exportToBigFile(final Path extractedFile, final UUID exportId, final long projectId, final RDFFormat rdfFormat) throws IOException
    {
        final var outputFile = exportIdToExportedPath(exportId, rdfFormat);

        final var input = new FileInputStream(extractedFile.toFile());
        final var output = new FileOutputStream(outputFile.toFile());

        final var writer = Rio.createWriter(rdfFormat, output);

        // TODO handle namespaces
        final var exportHandler = new RDFStreamExportHandler(writer, uriRemoveUniquenessHandler, prefixNodeService, projectId);

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