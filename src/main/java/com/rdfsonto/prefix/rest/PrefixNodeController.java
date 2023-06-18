package com.rdfsonto.prefix.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode;
import com.rdfsonto.prefix.service.PrefixNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/prefix")
public class PrefixNodeController
{
    private final PrefixNodeService prefixNodeService;

    @GetMapping
    ResponseEntity<?> getPrefixByProjectId(@RequestParam final long projectId)
    {
        return ResponseEntity.ok(prefixNodeService.findAll(projectId));
    }

    @PostMapping
    ResponseEntity<?> updatePrefix(@RequestBody final Map<String, String> prefixes, @RequestParam final long projectId)
    {
        return ResponseEntity.ok(prefixNodeService.save(projectId, prefixes));
    }

    @ExceptionHandler(ClassNodeException.class)
    public ResponseEntity<?> handle(final ClassNodeException classNodeException)
    {

        if (classNodeException.getErrorCode() == ClassNodeExceptionErrorCode.INTERNAL_ERROR)
        {
            classNodeException.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        log.warn(classNodeException.getMessage());
        return ResponseEntity.badRequest().body(classNodeException.getErrorCode());
    }
}
