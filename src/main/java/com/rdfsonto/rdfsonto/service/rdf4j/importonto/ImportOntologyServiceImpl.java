package com.rdfsonto.rdfsonto.service.rdf4j.importonto;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdfsonto.repository.importonto.ImportOntologyRepository;
import com.rdfsonto.rdfsonto.repository.importonto.ImportOntologyResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
class ImportOntologyServiceImpl implements ImportOntologyService
{
    private static final String WORKSPACE_DIR = "./workspace/";

    private final ImportOntologyRepository importOntologyRepository;

    @Override
    public DownloadedOntology downloadOntology(final URL source,
                                               final Long userId,
                                               final String projectName,
                                               final RDFFormat rdfFormat)
    {
        final var rdf4jDownloader = new RDF4JDownload(rdfFormat);

        final var ontologyTag = ontologyTag(userId, projectName);
        final var outputFile = Path.of(WORKSPACE_DIR + ontologyTag);

        try
        {
            rdf4jDownloader.prepareRDFFileToMergeIntoNeo4j(source, outputFile, ontologyTag);

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
    public ImportOntologyResult loadOntology(final DownloadedOntology downloadedOntology)
    {
        final var path = downloadedOntology.path().toString();
        final var rdfFormat = downloadedOntology.rdfFormat().getName();

        return importOntologyRepository.importOntology(path, rdfFormat);
    }

    private String ontologyTag(final Long userId, final String projectName)
    {
        return userId.toString() + projectName;
    }
}
