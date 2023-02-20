package com.rdfsonto.classnode.service;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder(setterPrefix = "with")
public class ClassNodeException extends RuntimeException
{
    private final ClassNodeExceptionErrorCode errorCode;

    public ClassNodeException(final String message, final ClassNodeExceptionErrorCode errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }
}
