package com.rdfsonto.importonto.service;

import java.net.URL;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.web.multipart.MultipartFile;

import com.rdfsonto.importonto.database.ImportOntologyResult;


public interface ImportOntologyService
{
    ImportOntologyResult importOntology(URL source, RDFFormat rdfFormat, Long userId, Long projectId);

    ImportOntologyResult importOntology(MultipartFile file, RDFFormat rdfFormat, Long userId, Long projectId);

}
