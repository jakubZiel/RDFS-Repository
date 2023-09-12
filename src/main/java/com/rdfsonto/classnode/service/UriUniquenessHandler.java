package com.rdfsonto.classnode.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class UriUniquenessHandler
{
    private static final String URI_KEY = "uri";
    private static final String USER_NAMESPACE_LABEL_PREFIX = "http://www.user_neo4j.com";

    ClassNode applyUniqueness(final ClassNode nonUniqueUriClassNode, final String projectTag)
    {
        final var nonUniqueUri = nonUniqueUriClassNode.uri();
        final var uniqueUri = applyUniqueness(nonUniqueUri, projectTag, true);

        final var uniqueLabelsStream = Stream.of("Resource", getClassNodeLabel(projectTag));
        final var labelsStream = nonUniqueUriClassNode.classLabels().stream()
            .map(label -> applyUniqueness(label, projectTag, true));

        final var uniqueLabels = Stream.concat(uniqueLabelsStream, labelsStream).toList();
        final var uniqueProperties = applyUniqueness(nonUniqueUriClassNode.properties(), projectTag);

        final var uniqueIncoming = applyUniquenessRelationships(nonUniqueUriClassNode.incomingNeighbours(), projectTag);
        final var uniqueOutgoing = applyUniquenessRelationships(nonUniqueUriClassNode.outgoingNeighbours(), projectTag);

        return nonUniqueUriClassNode.toBuilder()
            .withUri(uniqueUri)
            .withClassLabels(uniqueLabels)
            .withProperties(uniqueProperties)
            .withIncomingNeighbours(uniqueIncoming)
            .withOutgoingNeighbours(uniqueOutgoing)
            .build();
    }

    FilterCondition applyUniqueness(final FilterCondition filterCondition, final String projectTag)
    {
        final var property = filterCondition.property();
        return property.equals(URI_KEY) ?
            filterCondition.toBuilder().withValue(applyUniqueness(filterCondition.value(), projectTag, false)).build() :
            filterCondition.toBuilder().withProperty(applyUniqueness(filterCondition.property(), projectTag, false)).build();
    }

    List<String> applyUniqueness(final List<String> nonUniqueUris, final String projectTag)
    {
        return nonUniqueUris.stream().map(nonUniqueUri -> applyUniqueness(nonUniqueUri, projectTag, true)).toList();
    }

    List<String> addUniqueLabel(final List<String> labels, final String projectTag)
    {
        final var uniqueLabel = getClassNodeLabel(projectTag);
        return Stream.concat(Stream.of(uniqueLabel), labels.stream()).toList();
    }

    private Map<String, Object> applyUniqueness(final Map<String, Object> properties, final String projectTag)
    {
        return Optional.ofNullable(properties).map(nonNullProperties -> nonNullProperties.entrySet().stream()
                .map(property -> Map.entry(applyUniqueness(property.getKey(), projectTag, true), property.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);
    }

    private Map<Long, List<String>> applyUniquenessRelationships(final Map<Long, List<String>> neighbourhood, final String projectTag)
    {
        return Optional.ofNullable(neighbourhood).map(nonNulLNeighbourhood -> nonNulLNeighbourhood.entrySet().stream()
                .map(neighbourRelationships -> Map.entry(neighbourRelationships.getKey(), applyUniqueness(neighbourRelationships.getValue(), projectTag)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);
    }

    public String applyUniqueness(final String nonUniqueUri, final String projectTag, final boolean apply)
    {
        if (!UrlValidator.getInstance().isValid(nonUniqueUri))
        {
            return nonUniqueUri;
        }

        final var uri = Values.iri(nonUniqueUri);

        final var namespaceUrl = uri.getNamespace();
        final var localName = uri.getLocalName();

        return apply ? namespaceUrl + projectTag + localName : nonUniqueUri;
    }

    public String getClassNodeLabel(final String projectTag)
    {
        return USER_NAMESPACE_LABEL_PREFIX + "#" + projectTag;
    }

    public Optional<Long> getProjectIdFromLabels(final List<String> nodeLabels)
    {
        return nodeLabels.stream()
            .filter(label -> label.startsWith(USER_NAMESPACE_LABEL_PREFIX))
            .map(label -> label.replace(USER_NAMESPACE_LABEL_PREFIX + "#", ""))
            .map(tag -> Long.valueOf(tag.split("@")[2]))
            .findFirst();
    }
}

