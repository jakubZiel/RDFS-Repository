package com.rdfsonto.rdfsonto.importonto.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.importonto.database.ImportOntologyResult;
import com.rdfsonto.rdfsonto.project.service.ProjectService;
import com.rdfsonto.rdfsonto.rdf4j.RdfFormatParser;
import com.rdfsonto.rdfsonto.importonto.service.DownloadedOntology;
import com.rdfsonto.rdfsonto.importonto.service.ImportOntologyService;
import com.rdfsonto.rdfsonto.user.service.UserService;

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
        if (isNotValid(importOntologyRequest))
        {
            log.warn("Invalid import ontology request: {}", importOntologyRequest);
            return ResponseEntity.badRequest().body("invalid_request");
        }

        if (projectAlreadyExists(importOntologyRequest))
        {
            log.warn("Invalid imported ontology name: {}, ontology already exists", importOntologyRequest.projectName());
            return ResponseEntity.badRequest().body("invalid_project_name");
        }

        final var user = userService.findById(importOntologyRequest.userId());
        if (user.isEmpty())
        {
            log.warn("Attempted ontology import for non-existing user id: {}", importOntologyRequest.userId());
            return ResponseEntity.notFound().build();
        }

        final var rdfFormat = RdfFormatParser.parse(importOntologyRequest.rdfFormat());

        if (rdfFormat == null)
        {
            log.warn("Invalid rdf format: {}", importOntologyRequest.rdfFormat());
            return ResponseEntity.badRequest().body("invalid_rdf_format");
        }

        final var project = projectService.save(importOntologyRequest.projectName(), user.get());

        final var downloadedOntology = importOntologyService.downloadOntology(
            importOntologyRequest.source(),
            importOntologyRequest.userId(),
            project.getId(),
            rdfFormat
        );

        if (isNotDownloaded(downloadedOntology))
        {
            log.error("Failed to load, ontology request: {}, stacktrace: {}", importOntologyRequest, downloadedOntology.ioException());
            return ResponseEntity.internalServerError().body("failed_ontology_import");
        }

        final var importResult = importOntologyService.importOntology(downloadedOntology);

        if (isNotImported(importResult))
        {
            log.warn("Failed to import ontology request: {}, import response: {}", importOntologyRequest, importResult);
            return ResponseEntity.badRequest().body("ontology_import_error");
        }

        return ResponseEntity.ok().build();
    }

    private boolean isNotValid(final ImportOntologyRequest request)
    {
        return request.projectName() == null ||
            request.userId() == null ||
            request.source() == null ||
            request.rdfFormat() == null;
    }

    private boolean isNotImported(final ImportOntologyResult importOntologyResult)
    {
        return !importOntologyResult.getTerminationStatus().equals("OK") || importOntologyResult.getTriplesLoaded() <= 0;
    }

    private boolean projectAlreadyExists(final ImportOntologyRequest importOntologyRequest)
    {
        return projectService.findProjectByNameAndUserId(importOntologyRequest.projectName(), importOntologyRequest.userId()).isPresent();
    }

    private boolean isNotDownloaded(final DownloadedOntology downloadedOntology)
    {
        return downloadedOntology.ioException() != null;
    }
}