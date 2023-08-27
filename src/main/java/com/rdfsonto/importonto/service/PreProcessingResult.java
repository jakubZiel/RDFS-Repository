package com.rdfsonto.importonto.service;

import java.nio.file.Path;
import java.util.Map;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record PreProcessingResult(Path processedFile, Map<String, String> declaredNamespaces)
{
}
