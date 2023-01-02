package com.rdfsonto.rdfsonto.exportonto.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record ExportOntologyResult(File ontologyFile, BufferedInputStream inputStream, IOException ioException)
{
}
