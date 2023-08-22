package com.rdfsonto.exportonto.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.eclipse.rdf4j.rio.RDFFormat;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record ExtractedOntology(UUID exportId, RDFFormat rdfFormat, IOException ioException, boolean alreadyUntagged, File extractedFile)
{
}
