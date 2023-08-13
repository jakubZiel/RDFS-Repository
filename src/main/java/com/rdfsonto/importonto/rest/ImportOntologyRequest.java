package com.rdfsonto.importonto.rest;

import java.net.URL;

import org.springframework.web.multipart.MultipartFile;


public record ImportOntologyRequest(Long projectId, Long userId, URL source, String rdfFormat, MultipartFile file)
{
}
