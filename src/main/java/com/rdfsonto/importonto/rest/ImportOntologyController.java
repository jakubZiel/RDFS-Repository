package com.rdfsonto.importonto.rest;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
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

        return ResponseEntity.ok(
            importOntologyService.importOntology(
                importOntologyRequest.source(),
                rdfFormat,
                importOntologyRequest.userId(),
                importOntologyRequest.projectId()));
    }

    //TODO timeout when download takes too long
    @GetMapping("test-timeout")
    public void testTimeout() throws InterruptedException, ExecutionException
    {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final var fut = executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    System.err.println(System.currentTimeMillis());
                    Thread.sleep(2000);
                    System.err.println(System.currentTimeMillis());
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        });

        try
        {
            fut.get(3, SECONDS);
            System.out.println("holded for 3s");
        }
        catch (TimeoutException e)
        {
            System.err.println("interrupeted");
            System.err.println(System.currentTimeMillis());
            fut.cancel(true);
        }
    }



    @ExceptionHandler(ImportOntologyException.class)
    public ResponseEntity<?> handle(final ImportOntologyException importOntologyException)
    {
        log.warn(importOntologyException.getMessage());
        return ResponseEntity.badRequest().body(importOntologyException.getErrorCode());
    }

    private boolean isNotValid(final ImportOntologyRequest request)
    {
        return request.projectId() == null ||
            request.userId() == null ||
            request.source() == null ||
            request.rdfFormat() == null;
    }
}