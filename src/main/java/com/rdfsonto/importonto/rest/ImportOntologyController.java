package com.rdfsonto.importonto.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode;
import com.rdfsonto.importonto.service.ImportOntologyErrorCode;
import com.rdfsonto.importonto.service.ImportOntologyException;
import com.rdfsonto.importonto.service.ImportOntologyService;
import com.rdfsonto.infrastructure.security.service.AuthService;
import com.rdfsonto.rdf4j.RdfFormatParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/import")
public class ImportOntologyController
{
    private final AuthService authService;
    private final ImportOntologyService importOntologyService;

    @PostMapping
    public ResponseEntity<?> importOntology(@RequestBody final ImportOntologyRequest importOntologyRequest)
    {
        authService.validateProjectAccess(importOntologyRequest.projectId());

        final var rdfFormat = RdfFormatParser.parse(importOntologyRequest.rdfFormat());

        if (isNotValidUrl(importOntologyRequest))
        {
            log.warn("Invalid import ontology request: {}", importOntologyRequest);
            return ResponseEntity.badRequest().body(ImportOntologyErrorCode.INVALID_REQUEST);
        }

        return ResponseEntity.ok(
            importOntologyService.importOntology(
                importOntologyRequest.source(),
                rdfFormat,
                importOntologyRequest.userId(),
                importOntologyRequest.projectId()));
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importOntologyFile(final ImportOntologyRequest importOntologyRequest, final RedirectAttributes redirectAttributes)
    {
        authService.validateProjectAccess(importOntologyRequest.projectId());

        final var rdfFormat = RdfFormatParser.parse(importOntologyRequest.rdfFormat());
        redirectAttributes.addFlashAttribute("message", "Successfully uploaded file");

        if (isNotValidFile(importOntologyRequest))
        {
            log.warn("Invalid import ontology request: {}", importOntologyRequest);
            return ResponseEntity.badRequest().body(ImportOntologyErrorCode.INVALID_REQUEST);
        }

        final var start = System.currentTimeMillis();
        System.err.println(start);
        importOntologyService.importOntology(
            importOntologyRequest.file(),
            rdfFormat,
            importOntologyRequest.userId(),
            importOntologyRequest.projectId());

        final var end = System.currentTimeMillis();
        System.err.println(end);
        System.err.println(end - start);
        return ResponseEntity.ok().build();
    }

    private boolean isNotValidUrl(final ImportOntologyRequest request)
    {
        return request.projectId() == null ||
            request.userId() == null ||
            request.source() == null ||
            request.rdfFormat() == null;
    }

    private boolean isNotValidFile(final ImportOntologyRequest request)
    {
        return request.projectId() == null ||
            request.userId() == null ||
            request.file() == null || request.file().isEmpty() ||
            request.rdfFormat() == null;
    }

    @ExceptionHandler(ImportOntologyException.class)
    public ResponseEntity<?> handle(final ImportOntologyException importOntologyException)
    {
        log.warn(importOntologyException.getMessage());
        return ResponseEntity.badRequest().body(importOntologyException.getErrorCode());
    }

    @ExceptionHandler(ClassNodeException.class)
    public ResponseEntity<?> handle(final ClassNodeException classNodeException)
    {
        if (classNodeException.getErrorCode() == ClassNodeExceptionErrorCode.UNAUTHORIZED_RESOURCE_ACCESS)
        {
            log.warn("Unauthorized import access to project", classNodeException);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.internalServerError().build();
    }
}