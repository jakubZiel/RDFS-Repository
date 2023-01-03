package com.rdfsonto.exportonto.rest;

public record ExportOntologyRequest(String fileName, String rdfFormat, String projectName, Long userId)
{
}
