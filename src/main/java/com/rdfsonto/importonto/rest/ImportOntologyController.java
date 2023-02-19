package com.rdfsonto.importonto.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.importonto.service.ImportOntologyErrorCode;
import com.rdfsonto.importonto.service.ImportOntologyException;
import com.rdfsonto.importonto.service.ImportOntologyService;
import com.rdfsonto.rdf4j.RdfFormatParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/import")
public class ImportOntologyController
{
    private final ImportOntologyService importOntologyService;

    @PostMapping
    public ResponseEntity<?> importOntology(@RequestBody final ImportOntologyRequest importOntologyRequest)
    {
        final var rdfFormat = RdfFormatParser.parse(importOntologyRequest.rdfFormat());

        if (isNotValid(importOntologyRequest))
        {
            log.warn("Invalid import ontology request: {}", importOntologyRequest);
            return ResponseEntity.badRequest().body(ImportOntologyErrorCode.INVALID_REQUEST);
        }

        try
        {
            return ResponseEntity.ok(
                importOntologyService.importOntology(
                    importOntologyRequest.source(),
                    rdfFormat,
                    importOntologyRequest.userId(),
                    importOntologyRequest.projectId()));
        }
        catch (final ImportOntologyException importOntologyException)
        {
            log.warn(importOntologyException.getMessage());
            return ResponseEntity.badRequest().body(importOntologyException.getErrorCode());
        }
    }

    private boolean isNotValid(final ImportOntologyRequest request)
    {
        return request.projectId() == null ||
            request.userId() == null ||
            request.source() == null ||
            request.rdfFormat() == null;
    }
}