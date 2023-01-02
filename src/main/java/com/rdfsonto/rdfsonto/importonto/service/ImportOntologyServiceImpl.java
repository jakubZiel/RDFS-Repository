package com.rdfsonto.rdfsonto.importonto.service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdfsonto.importonto.database.ImportOntologyRepository;
import com.rdfsonto.rdfsonto.importonto.database.ImportOntologyResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
class ImportOntologyServiceImpl implements ImportOntologyService
{
    private static final String WORKSPACE_DIR = System.getProperty("user.dir") + "/workspace/";

    private final ImportOntologyRepository importOntologyRepository;

    @Override
    public DownloadedOntology downloadOntology(final URL source,
                                               final Long userId,
                                               final Long projectId,
                                               final RDFFormat rdfFormat)
    {
        final var rdf4jDownloader = new RDFImporter();

        final var ontologyTag = ontologyTag(userId, projectId);
        final var outputFile = Path.of(WORKSPACE_DIR + ontologyTag + ".input");

        try
        {
            rdf4jDownloader.prepareRDFFileToMergeIntoNeo4j(source, outputFile, ontologyTag, rdfFormat);

            return DownloadedOntology.builder()
                .withPath(outputFile)
                .withRdfFormat(rdfFormat)
                .build();
        }
        catch (final IOException ioException)
        {
            return DownloadedOntology.builder()
                .withIoException(ioException)
                .build();
        }
    }

    @Override
    public ImportOntologyResult importOntology(final DownloadedOntology downloadedOntology)
    {
        final var path = "file://" + getRemoteWorkspaceDir(downloadedOntology.path().toString());
        final var rdfFormat = downloadedOntology.rdfFormat().getName();

        return importOntologyRepository.importOntology(path, rdfFormat);
    }

    private String getRemoteWorkspaceDir(final String localWorkspaceDir)
    {
        return localWorkspaceDir.substring(localWorkspaceDir.indexOf("/workspace"));
    }

    private String ontologyTag(final Long userId, final Long projectId)
    {
        return userId.toString() + "_" + projectId.toString();
    }
}
