package com.rdfsonto.importonto.service;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder(setterPrefix = "with")
public class ImportOntologyException extends RuntimeException
{
    private final ImportOntologyErrorCode errorCode;

    public ImportOntologyException(final String message, final ImportOntologyErrorCode errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }
}