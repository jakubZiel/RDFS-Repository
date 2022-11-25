package com.rdfsonto.rdfsonto.service.rdf4j.exportonto;

import java.io.BufferedInputStream;
import java.io.File;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.data.util.Pair;


public interface ExportOntologyService
{
    Pair<File, BufferedInputStream> exportOntology(Long userId, String projectName, RDFFormat rdfFormat);
}
