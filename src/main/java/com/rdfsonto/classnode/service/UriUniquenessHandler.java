package com.rdfsonto.classnode.service;

import org.springframework.stereotype.Component;

import com.rdfsonto.project.service.ProjectService;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class UriUniquenessHandler
{
    private final ProjectService projectService;
    private final UniqueUriIdHandler uniqueUriIdHandler;

    public ClassNode removeUniqueness(final ClassNode uniqueUriClassNode)
    {
        final var nonUniqueUri = uniqueUriIdHandler.extractUri(uniqueUriClassNode);

        return uniqueUriClassNode.toBuilder()
            .withUri(nonUniqueUri)
            .build();
    }

    public ClassNode applyUniqueness(final ClassNode nonUniqueUriClassNode, final String projectTag)
    {
        final var nonUniqueUri = nonUniqueUriClassNode.uri();
        final var index = nonUniqueUri.lastIndexOf("#");
        final var uniqueUri = nonUniqueUri.substring(0, index) + projectTag + "#";

        return nonUniqueUriClassNode.toBuilder()
            .withUri(uniqueUri)
            .build();
    }
}

