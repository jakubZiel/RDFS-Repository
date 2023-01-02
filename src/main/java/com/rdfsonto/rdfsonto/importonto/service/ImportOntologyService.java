package com.rdfsonto.rdfsonto.importonto.service;

import java.net.URL;

import org.eclipse.rdf4j.rio.RDFFormat;

import com.rdfsonto.rdfsonto.importonto.database.ImportOntologyResult;


public interface ImportOntologyService
{
    DownloadedOntology downloadOntology(URL source, Long userId, Long projectId, RDFFormat rdfFormat);

    ImportOntologyResult importOntology(DownloadedOntology downloadedOntology);
}
