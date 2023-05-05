package com.rdfsonto.classnode.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class UriUniquenessHandler
{
    private static final String URI_KEY = "uri";
    private static final String USER_NAMESPACE_LABEL_PREFIX = "http://www.user_neo4j.com";
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
        final var uniqueUri = applyUniqueness(nonUniqueUri, projectTag);

        nonUniqueUriClassNode.classLabels().add(getClassNodeLabel(projectTag));
        nonUniqueUriClassNode.classLabels().add("Resource");

        return nonUniqueUriClassNode.toBuilder()
            .withUri(uniqueUri)
            .withClassLabels(nonUniqueUriClassNode.classLabels())
            .build();
    }

    public List<FilterCondition> applyUniqueness(final List<FilterCondition> filterConditions, final String projectTag)
    {
        return filterConditions.stream()
            .map(condition -> applyUniqueness(condition, projectTag))
            .toList();
    }

    public List<String> addUniqueLabel(final List<String> labels, final String projectTag)
    {
        final var uniqueLabel = getClassNodeLabel(projectTag);
        return Stream.concat(Stream.of(uniqueLabel), labels.stream()).toList();
    }

    public String getClassNodeLabel(final String projectTag)
    {
        return USER_NAMESPACE_LABEL_PREFIX + "#" + projectTag;
    }

    private FilterCondition applyUniqueness(final FilterCondition filterCondition, final String projectTag)
    {
        final var property = filterCondition.property();
        return property.equals(URI_KEY) ? filterCondition.toBuilder()
            .withValue(applyUniqueness(filterCondition.value(), projectTag))
            .build() : filterCondition;
    }

    public String applyUniqueness(final String nonUniqueUri, final String projectTag)
    {
        return nonUniqueUri.replace("#", projectTag + "#");
    }

    public String removeUniqueness(final String uniqueUri)
    {
        return uniqueUriIdHandler.removeUniqueness(uniqueUri);
    }
}

