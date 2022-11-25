package com.rdfsonto.rdfsonto.controller.exportonto;

public record ExportOntologyRequest(String fileName, String rdfFormat, String projectName, Long userId)
{
}
