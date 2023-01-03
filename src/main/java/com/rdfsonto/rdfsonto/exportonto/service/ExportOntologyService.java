package com.rdfsonto.rdfsonto.exportonto.service;

import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;


public interface ExportOntologyService
{
    ExportOntologyResult exportOntology(Long userId, Long projectId, ExtractedOntology extractedOntology);

    ExtractedOntology extractOntology(Long userId, Long projectId, RDFFormat rdfFormat);
}
