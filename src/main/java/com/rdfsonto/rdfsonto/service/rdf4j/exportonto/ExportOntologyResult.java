package com.rdfsonto.rdfsonto.service.rdf4j.exportonto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record ExportOntologyResult(File ontologyFile, BufferedInputStream inputStream, IOException ioException)
{
}
