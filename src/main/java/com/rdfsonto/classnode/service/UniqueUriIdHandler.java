package com.rdfsonto.classnode.service;

import org.springframework.stereotype.Component;


@Component
public class UniqueUriIdHandler
{
    private static final String UNIQUE_URI_PATTERN = "@%s@%s@";
    private static final String UNIQUE_URI_REGEX = "@.+@.+@";

    public String uniquerUriTag(final long userId, final long projectId)
    {
        return UNIQUE_URI_PATTERN.formatted(userId, projectId);
    }

    public String extractUri(final ClassNode classNode)
    {
        return classNode.uri().replaceFirst(UNIQUE_URI_REGEX, "");
    }

    public String removeUniqueness(final String uniqueUri) {
        return uniqueUri.replaceFirst(UNIQUE_URI_REGEX, "");
    }
}