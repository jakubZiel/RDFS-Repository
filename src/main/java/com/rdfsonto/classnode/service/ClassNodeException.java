package com.rdfsonto.classnode.service;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder(setterPrefix = "with")
public class ClassNodeException extends RuntimeException
{
    private final String message;
    private final ClassNodeExceptionErrorCode errorCode;
}
