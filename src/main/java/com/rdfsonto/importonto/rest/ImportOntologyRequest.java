package com.rdfsonto.importonto.rest;

import java.net.URL;


public record ImportOntologyRequest(Long projectId, Long userId, URL source, String rdfFormat)
{
}
