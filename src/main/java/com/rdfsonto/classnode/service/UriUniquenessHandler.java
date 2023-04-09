package com.rdfsonto.classnode.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class UriUniquenessHandler
{
    private static final String URI_KEY = "uri";
    private static final String USER_NAMESPACE_LABEL_PREFIX = "https://www.user_neo4j";
    private final UniqueUriIdHandler uniqueUriIdHandler;

    public ClassNode removeUniqueness(final ClassNode uniqueUriClassNode)
    {
        final var nonUniqueUri = uniqueUriIdHandler.extractUri(uniqueUriClassNode);
        final var nodeLabels = uniqueUriClassNode.classLabels().stream()
            .filter(label -> !label.startsWith(USER_NAMESPACE_LABEL_PREFIX))
            .toList();

        final var properties = Optional.ofNullable(uniqueUriClassNode.properties())
            .map(props -> props.entrySet().stream()
                .filter(property -> !property.getKey().equals(URI_KEY))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);

        return uniqueUriClassNode.toBuilder()
            .withUri(nonUniqueUri)
            .withClassLabels(nodeLabels)
            .withProperties(properties)
            .build();
    }

    public ClassNode applyUniqueness(final ClassNode nonUniqueUriClassNode, final String projectTag)
    {
        final var nonUniqueUri = nonUniqueUriClassNode.uri();
        final var uniqueUri = nonUniqueUri.replace("#", projectTag + "#");

        nonUniqueUriClassNode.classLabels().add(getClassNodeLabel(projectTag));
        nonUniqueUriClassNode.classLabels().add("Resource");

        return nonUniqueUriClassNode.toBuilder()
            .withUri(uniqueUri)
            .withClassLabels(nonUniqueUriClassNode.classLabels())
            .build();
    }

    public String getClassNodeLabel(final String projectTag)
    {
        return USER_NAMESPACE_LABEL_PREFIX + "#" + projectTag;
    }
}

