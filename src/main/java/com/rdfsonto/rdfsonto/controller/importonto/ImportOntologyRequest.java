package com.rdfsonto.rdfsonto.controller.importonto;

import java.net.URL;


public record ImportOntologyRequest(String projectName, Long userId, URL source, String rdfFormat)
{
}
