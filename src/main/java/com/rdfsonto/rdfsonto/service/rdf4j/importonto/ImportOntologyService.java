package com.rdfsonto.rdfsonto.service.rdf4j.importonto;

import java.net.URL;

import org.eclipse.rdf4j.rio.RDFFormat;

import com.rdfsonto.rdfsonto.repository.importonto.ImportOntologyResult;


public interface ImportOntologyService
{
    DownloadedOntology downloadOntology(URL source, Long userId, String projectName, RDFFormat rdfFormat);

    ImportOntologyResult loadOntology(DownloadedOntology downloadedOntology);
}
