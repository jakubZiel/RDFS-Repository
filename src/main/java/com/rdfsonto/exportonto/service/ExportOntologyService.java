package com.rdfsonto.exportonto.service;

import org.eclipse.rdf4j.rio.RDFFormat;


public interface ExportOntologyService
{
    ExportOntologyResult exportOntology(long userId, long projectId, RDFFormat rdfFormat);
}
