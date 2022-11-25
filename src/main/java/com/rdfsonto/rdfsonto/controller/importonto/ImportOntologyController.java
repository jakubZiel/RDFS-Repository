package com.rdfsonto.rdfsonto.controller.importonto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.repository.importonto.ImportOntologyResponse;
import com.rdfsonto.rdfsonto.service.project.ProjectService;
import com.rdfsonto.rdfsonto.service.rdf4j.RdfFormatParser;
import com.rdfsonto.rdfsonto.service.rdf4j.importonto.ImportOntologyService;
import com.rdfsonto.rdfsonto.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/import")
public class ImportOntologyController
{
    private final ProjectService projectService;
    private final UserService userService;
    private final ImportOntologyService importOntologyService;

    @PostMapping
    public ResponseEntity<?> importOntology(@RequestBody final ImportOntologyRequest importOntologyRequest)
    {
        if (validate(importOntologyRequest))
        {
            log.warn("Invalid import ontology request: {}", importOntologyRequest);
            return ResponseEntity.badRequest().body("invalid_request");
        }

        if (projectService.findProjectByNameAndUserId(importOntologyRequest.projectName(), importOntologyRequest.userId()).isPresent())
        {
            log.warn("Invalid imported ontology name: {}, ontology already exists", importOntologyRequest.projectName());
            return ResponseEntity.badRequest().body("invalid_project_name");
        }

        if (userService.findById(importOntologyRequest.userId()).isEmpty())
        {
            log.warn("Attempted ontology import for non-existing user id: {}", importOntologyRequest.userId());
            return ResponseEntity.notFound().build();
        }

        final var rdfFormat =  RdfFormatParser.parse(importOntologyRequest.rdfFormat());

        if (rdfFormat == null)
        {
            log.warn("Invalid rdf format: {}", importOntologyRequest.rdfFormat());
            return ResponseEntity.badRequest().body("invalid_rdf_format");
        }

        final var downloadedOntology = importOntologyService.downloadOntology(
            importOntologyRequest.source(),
            importOntologyRequest.userId(),
            importOntologyRequest.projectName(),
            rdfFormat
        );

        final var importResult = importOntologyService.loadOntology(downloadedOntology);

        if (!isImported(importResult))
        {
            log.warn("Not successful import: {}", importResult);
            return ResponseEntity.badRequest().body("ontology_import_error");
        }

        return ResponseEntity.ok().build();
    }

    private boolean validate(final ImportOntologyRequest request)
    {
        return (request.projectName() != null &&
            request.userId() != null &&
            request.source() != null &&
            request.rdfFormat() != null);
    }

    private boolean isImported(final ImportOntologyResponse importOntologyResponse)
    {
        return true;
    }
}