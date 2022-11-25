package com.rdfsonto.rdfsonto.service.rdf4j.importonto;

import java.net.URL;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdfsonto.repository.importonto.ImportOntologyRepository;
import com.rdfsonto.rdfsonto.repository.importonto.ImportOntologyResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
class ImportOntologyServiceImpl implements ImportOntologyService
{
    private final ImportOntologyRepository importOntologyRepository;

    @Override
    public DownloadedOntology downloadOntology(final URL source,
                                               final Long userId,
                                               final String projectName,
                                               final RDFFormat rdfFormat)
    {
        final var rdf4jDownloader = new RDF4JDownload(rdfFormat);

        return null;
    }

    @Override
    public ImportOntologyResponse loadOntology(final DownloadedOntology downloadedOntology)
    {
        //return importOntologyRepository.importOntology();
        return null;
    }
}
