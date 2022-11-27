package com.rdfsonto.rdfsonto.service.rdf4j.importonto;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.rdf4j.rio.RDFFormat;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record DownloadedOntology(Path path, RDFFormat rdfFormat, IOException ioException)
{
}
