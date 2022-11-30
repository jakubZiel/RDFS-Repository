package com.rdfsonto.rdfsonto.service.rdf4j.exportonto;

import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;


public interface ExportOntologyService
{
    ExportOntologyResult exportOntology(Long userId, Long projectId, ExtractedOntology extractedOntology);

    ExtractedOntology extractOntology(Long userId, Long projectId, RDFFormat rdfFormat);

    void clearWorkspace(Path filePath);
}
