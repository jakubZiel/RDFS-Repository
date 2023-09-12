package com.rdfsonto.exportonto.rest;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_REQUEST;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode;
import com.rdfsonto.exportonto.service.ExportOntologyService;
import com.rdfsonto.infrastructure.security.service.AuthService;
import com.rdfsonto.rdf4j.RdfFormatParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/export")
public class ExportOntologyController
{
    private final AuthService authService;
    private final ExportOntologyService exportOntologyService;

    @PostMapping
    public ResponseEntity<?> exportRdfResource(@RequestBody final ExportOntologyRequest exportOntologyRequest)
    {
        authService.validateProjectAccess(exportOntologyRequest.projectId());

        if (isInvalid(exportOntologyRequest))
        {
            log.warn("Invalid export ontology request: {}", exportOntologyRequest);
            return ResponseEntity.badRequest().body(INVALID_REQUEST);
        }

        final var rdfFormat = RdfFormatParser.parse(exportOntologyRequest.rdfFormat());
        final var ontologyExport = exportOntologyService.exportOntology(exportOntologyRequest.userId(), exportOntologyRequest.projectId(), rdfFormat);

        return ResponseEntity.ok(ontologyExport.exportedOntologyFile().getName());
    }

    @GetMapping(value = "/file/{userId}/{projectId}")
    public void getFile(@PathVariable final Long projectId, @PathVariable final Long userId, final HttpServletResponse response) throws IOException
    {
        authService.validateProjectAccess(projectId);

        final var snapshot = exportOntologyService.provideExportedSnapshot(userId, projectId);
        final var inputStream = snapshot.fileInputStream();

        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment; filename=" + snapshot.fileName() + ".gz");

        final var out = response.getOutputStream();

        IOUtils.copy(inputStream, out);
        out.close();
        inputStream.close();
    }

    @ExceptionHandler(ClassNodeException.class)
    public ResponseEntity<?> handle(final ClassNodeException classNodeException)
    {
        if (classNodeException.getErrorCode() == ClassNodeExceptionErrorCode.UNAUTHORIZED_RESOURCE_ACCESS)
        {
            log.warn("Unauthorized access to project", classNodeException);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (classNodeException.getErrorCode() == ClassNodeExceptionErrorCode.INTERNAL_ERROR)
        {
            log.error("Class node exception", classNodeException);
            return ResponseEntity.internalServerError().build();
        }

        log.warn(classNodeException.getMessage());
        return ResponseEntity.badRequest().body(classNodeException.getErrorCode());
    }

    private boolean isInvalid(final ExportOntologyRequest request)
    {
        return (request.projectId() == null ||
            request.userId() == null ||
            request.fileName() == null ||
            request.rdfFormat() == null);
    }
}