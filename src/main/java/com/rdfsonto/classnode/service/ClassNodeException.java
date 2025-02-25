package com.rdfsonto.classnode.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
@Builder(setterPrefix = "with")
public class ClassNodeException extends RuntimeException
{
    private final String message;
    private final ClassNodeExceptionErrorCode errorCode;
    private final Object value;

    public ClassNodeException(final String message, final ClassNodeExceptionErrorCode errorCode)
    {
        this.message = message;
        this.errorCode = errorCode;
        this.value = null;
    }
}
