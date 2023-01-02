package com.rdfsonto.rdfsonto.importonto.rest;

import java.net.URL;


public record ImportOntologyRequest(String projectName, Long userId, URL source, String rdfFormat)
{
}
