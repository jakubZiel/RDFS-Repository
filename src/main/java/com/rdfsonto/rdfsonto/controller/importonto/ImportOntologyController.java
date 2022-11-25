package com.rdfsonto.rdfsonto.controller.importonto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.service.project.ProjectService;
import com.rdfsonto.rdfsonto.service.rdf4j.download.RDF4JDownloadService;
import com.rdfsonto.rdfsonto.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/import")
public class ImportController
{
    private final ProjectService projectService;
    private final UserService userService;
    private final RDF4JDownloadService downloadService;

    @PostMapping
    ResponseEntity<?> importOntology(@RequestBody final ImportOntologyRequest importOntologyRequest)
    {


        return ResponseEntity.ok(null);
    }
}


