package com.rdfsonto.importonto.service;

import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.FAILED_ONTOLOGY_IMPORT;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_ONTOLOGY_URL;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_PROJECT_ID;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_RDF_FORMAT;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_REQUEST;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_USER_ID;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.elastic.service.ElasticSearchClassNodeBulkService;
import com.rdfsonto.importonto.database.ImportOntologyRepository;
import com.rdfsonto.importonto.database.ImportOntologyResult;
import com.rdfsonto.prefix.service.PrefixNodeService;
import com.rdfsonto.project.database.ProjectNode;
import com.rdfsonto.project.service.ProjectService;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
class ImportOntologyServiceImpl implements ImportOntologyService
{
    private static final String WORKSPACE_DIR = System.getProperty("user.dir") + "/workspace/";
    private static final long MAX_ONTOLOGY_FILE_SIZE_BYTES = 10_000_000;

    private final UserService userService;
    private final ImportOntologyRepository importOntologyRepository;
    private final ProjectService projectService;
    private final PrefixNodeService prefixNodeService;
    private final UniqueUriIdHandler uniqueUriIdHandler;
    private final ReferencedResourceHandler referencedResourceHandler;
    private final ElasticSearchClassNodeBulkService elasticSearchClassNodeBulkService;

    @Override
    public ImportOntologyResult importOntology(final URL source, final RDFFormat rdfFormat, final Long userId, final Long projectId)
    {
        userService.findById(userId)
            .orElseThrow(() -> new ImportOntologyException("User with ID: %s does not exist.".formatted(userId), INVALID_USER_ID));

        final var validRdfFormat = Optional.ofNullable(rdfFormat)
            .orElseThrow(() -> new ImportOntologyException("Invalid RDF format", INVALID_RDF_FORMAT));

        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ImportOntologyException("Project with ID: %s does not exist.".formatted(projectId), INVALID_PROJECT_ID));

        final var downloadedOntology = downloadOntology(source, project, validRdfFormat);

        if (downloadedOntology.ioException() != null)
        {
            throw new ImportOntologyException("Failed to download ontology form URL: %s.".formatted(source), INVALID_ONTOLOGY_URL);
        }

        log.info("Started importing ontology from URL : {}", source);
        final var importResult = importOntology(downloadedOntology);
        referencedResourceHandler.findAndLabelReferencedResources(projectId);

        if (!importResult.getTerminationStatus().equals("OK") || importResult.getTriplesLoaded() <= 0)
        {
            throw new ImportOntologyException("Failed to import ontology.", FAILED_ONTOLOGY_IMPORT);
        }

        log.info("Started indexing ontology from URL : {}", source);
        elasticSearchClassNodeBulkService.createIndex(userId, projectId);
        log.info("Imported into Elasticsearch.");
        return importResult;
    }

    @Override
    public ImportOntologyResult importOntology(final MultipartFile file, final RDFFormat rdfFormat, final Long userId, final Long projectId)
    {
        userService.findById(userId)
            .orElseThrow(() -> new ImportOntologyException("User with ID: %s does not exist.".formatted(userId), INVALID_USER_ID));

        final var validRdfFormat = Optional.ofNullable(rdfFormat)
            .orElseThrow(() -> new ImportOntologyException("Invalid RDF format", INVALID_RDF_FORMAT));

        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ImportOntologyException("Project with ID: %s does not exist.".formatted(projectId), INVALID_PROJECT_ID));

        if (file.isEmpty())
        {
            throw new ImportOntologyException("Failed to upload file with data.", INVALID_REQUEST);
        }

        log.info("Started importing ontology from file : {}", file.getName());
        final var ontologyTag = projectService.getProjectTag(project);
        try
        {
            final var rdf4jStreamDownloader = new RDFStreamImporter();
            final var output = rdf4jStreamDownloader.getProcessedRdfFileForNeo4j(file, WORKSPACE_DIR, ontologyTag, validRdfFormat);

            final var downloadedOntology = DownloadedOntology.builder()
                .withPath(output)
                .withRdfFormat(validRdfFormat)
                .build();

            log.info("Started importing ontology from file : {}", file.getName());
            final var importResult = importOntology(downloadedOntology);
            referencedResourceHandler.findAndLabelReferencedResources(projectId);

            if (!importResult.getTerminationStatus().equals("OK") || importResult.getTriplesLoaded() <= 0)
            {
                throw new ImportOntologyException("Failed to import ontology: %s.".formatted(importResult), FAILED_ONTOLOGY_IMPORT);
            }

            log.info("Started indexing ontology from file : {}", file.getName());
            elasticSearchClassNodeBulkService.createIndex(userId, projectId);
            log.info("Imported file into Elasticsearch");
            return importResult;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new IllegalStateException("Failed to upload file.");
        }
    }

    public DownloadedOntology downloadOntology(final URL source,
                                               final ProjectNode project,
                                               final RDFFormat rdfFormat)
    {
        final long fileSize = getRemoteFileSize(source);
        final var ontologyTag = projectService.getProjectTag(project);

        try
        {
            if (true)
            {
                final var rdf4jStreamDownloader = new RDFStreamImporter();

                final var outputFile = rdf4jStreamDownloader.getProcessedRdfFileForNeo4j(source, WORKSPACE_DIR, ontologyTag, rdfFormat);

                return DownloadedOntology.builder()
                    .withPath(outputFile)
                    .withRdfFormat(rdfFormat)
                    .build();
            }
            else
            {
                final var outputFile = Path.of(WORKSPACE_DIR + ontologyTag + ".output");
                final var rdf4jDownloader = new RDFImporter();

                rdf4jDownloader.prepareRDFFileToMergeIntoNeo4j(source, outputFile, ontologyTag, rdfFormat);
                importPrefixes(rdf4jDownloader, project.getId());

                return DownloadedOntology.builder()
                    .withPath(outputFile)
                    .withRdfFormat(rdfFormat)
                    .build();
            }
        }
        catch (final IOException ioException)
        {
            return DownloadedOntology.builder()
                .withIoException(ioException)
                .build();
        }
    }

    private ImportOntologyResult importOntology(final DownloadedOntology downloadedOntology)
    {
        final var path = "file://" + getWorkspaceDirAbsolutePath(downloadedOntology.path().toString());
        final var rdfFormat = downloadedOntology.rdfFormat().getName();

        return importOntologyRepository.importOntology(path, rdfFormat);
    }

    private void importPrefixes(final RDFImporter rdf4jDownloader, final long projectId)
    {
        final var importNamespaces = rdf4jDownloader.getLoadedNamespaces().stream()
            .map(x -> Map.entry(x.getPrefix(), uniqueUriIdHandler.removeUniqueness(x.getName())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final var newNamespaces = prefixNodeService.findAll(projectId)
            .map(prefixMapping -> updateNamespace(prefixMapping.prefixToUri(), importNamespaces))
            .orElse(importNamespaces);

        prefixNodeService.save(projectId, newNamespaces);
    }

    private Map<String, String> updateNamespace(final Map<String, String> currentNamespaces, final Map<String, String> updateNamespace)
    {
        return Stream.of(currentNamespaces, updateNamespace)
            .flatMap(namespace -> namespace.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (original, update) -> original));
    }

    private String getWorkspaceDirAbsolutePath(final String localWorkspaceDir)
    {
        return localWorkspaceDir.substring(localWorkspaceDir.indexOf("/workspace"));
    }

    private Long getRemoteFileSize(final URL source)
    {
        final var client = WebClient.create(source.toString());
        final var headMethod = client.head();

        final var responseHeaders = headMethod.retrieve()
            .toEntity(String.class)
            .map(HttpEntity::getHeaders)
            .block();

        final var contentLength = Optional.ofNullable(responseHeaders).map(HttpHeaders::getContentLength).orElse(-1L);

        return contentLength >= 0 ? contentLength : getRemoteFileSizeHttpConnection(source.toString());
    }

    private Integer getRemoteFileSizeHttpConnection(final String url)
    {
        try
        {
            final var connection = (HttpURLConnection) (new URL(url)).openConnection();
            final var contentLength = connection.getContentLength();
            connection.disconnect();

            return contentLength;
        }
        catch (final IOException ioException)
        {
            throw new IllegalStateException("Can not check size of an ontology.");
        }
    }
}
