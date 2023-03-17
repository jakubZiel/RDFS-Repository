package com.rdfsonto.importonto.service;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder(setterPrefix = "with")
public class ImportOntologyException extends RuntimeException
{
    private final String message;
    private final ImportOntologyErrorCode errorCode;
}