package com.rdfsonto.exportonto.service;

import org.eclipse.rdf4j.rio.RDFFormat;


public interface ExportOntologyService
{
    ExportOntologyResult exportOntology(Long userId, Long projectId, ExtractedOntology extractedOntology);

    ExtractedOntology extractOntology(Long userId, Long projectId, RDFFormat rdfFormat);
}
