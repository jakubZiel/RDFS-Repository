package com.rdfsonto.exportonto.rest;

public record ExportOntologyRequest(String fileName, String rdfFormat, Long projectId, Long userId)
{
}
