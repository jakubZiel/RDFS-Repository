package com.rdfsonto.importonto.service;

import java.net.URL;

import org.eclipse.rdf4j.rio.RDFFormat;

import com.rdfsonto.importonto.database.ImportOntologyResult;


public interface ImportOntologyService
{
    ImportOntologyResult importOntology(URL source, RDFFormat rdfFormat, Long userId, Long projectId);
}
