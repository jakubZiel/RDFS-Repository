package com.rdfsonto.exportonto.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record ExportOntologyResult(UUID exportId, File exportedOntologyFile, BufferedInputStream inputStream)
{
}
