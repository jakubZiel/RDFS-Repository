package com.rdfsonto.classnode.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class ClassNodeValidator
{
    // TODO
    void validate(final ClassNode classNode) throws ClassNodeException
    {
        Optional.ofNullable(classNode)
            .filter(this::hasLabels)
            .orElseThrow(() -> new ClassNodeException("Node invalid for save operation.", ClassNodeExceptionErrorCode.INVALID_REQUEST));
    }

    boolean hasLabels(final ClassNode classNode)
    {
        return classNode.classLabels() != null && !classNode.classLabels().isEmpty();
    }
}