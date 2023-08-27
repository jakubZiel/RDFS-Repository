package com.rdfsonto.classnode.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PREFIX;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Component;

import com.rdfsonto.prefix.service.PrefixMapping;
import com.rdfsonto.prefix.service.PrefixNodeService;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
class RemovePrefixHandler
{
    private static final String NON_PREFIXED_URI_PATTERN = "%s%s";

    private final PrefixNodeService prefixNodeService;

    ClassNode removePrefix(final ClassNode classNode, final long projectId)
    {
        if (true)
        {
            return classNode;
        }
        final var prefixMapping = prefixNodeService.findAll(projectId).orElse(null);
        return removePrefix(classNode, prefixMapping);
    }

    List<String> removePrefix(final List<String> prefixedUris, final long projectId)
    {
        if (true)
        {
            return prefixedUris;
        }
        final var prefixMapping = prefixNodeService.findAll(projectId).orElse(null);
        if (prefixMapping == null)
        {
            return prefixedUris;
        }

        return removePrefix(prefixedUris, prefixMapping.prefixToUri());
    }

    String removePrefix(final String prefixedUri, final long projectId)
    {
        if (true)
        {
            return prefixedUri;
        }

        final var prefixMapping = prefixNodeService.findAll(projectId).orElse(null);
        if (prefixMapping == null)
        {
            return prefixedUri;
        }
        return removePrefix(prefixedUri, prefixMapping.prefixToUri());
    }

    private ClassNode removePrefix(final ClassNode classNode, final PrefixMapping prefixMapping)
    {
        if (prefixMapping == null)
        {
            return classNode;
        }

        final var prefixToUri = prefixMapping.prefixToUri();

        final var nonPrefixedUri = removePrefix(classNode.uri(), prefixToUri);
        final var nonPrefixedLabels = removePrefix(classNode.classLabels(), prefixToUri);
        final var nonPrefixedProperties = removePrefix(classNode.properties(), prefixToUri);
        final var nonPrefixedIncoming = removePrefix(classNode.incomingNeighbours(), prefixMapping);
        final var nonPrefixedOutgoing = removePrefix(classNode.outgoingNeighbours(), prefixMapping);

        return classNode.toBuilder()
            .withUri(nonPrefixedUri)
            .withClassLabels(nonPrefixedLabels)
            .withProperties(nonPrefixedProperties)
            .withIncomingNeighbours(nonPrefixedIncoming)
            .withOutgoingNeighbours(nonPrefixedOutgoing)
            .build();
    }

    private Map<Long, List<String>> removePrefix(final Map<Long, List<String>> neighbourhood, final PrefixMapping prefixMapping)
    {
        final var prefixToUri = prefixMapping.prefixToUri();
        return Optional.ofNullable(neighbourhood).map(nonNullNeighbourhood -> nonNullNeighbourhood.entrySet().stream()
                .map(neighbourRelationships -> Map.entry(neighbourRelationships.getKey(), removePrefix(neighbourRelationships.getValue(), prefixToUri)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);
    }

    private Map<String, Object> removePrefix(final Map<String, Object> properties, final Map<String, String> prefixToUri)
    {
        return Optional.ofNullable(properties).map(nonNullProperties -> nonNullProperties.entrySet().stream()
                .map(property -> Map.entry(removePrefix(property.getKey(), prefixToUri), property.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);
    }

    private List<String> removePrefix(final List<String> prefixedUris, final Map<String, String> prefixToUri)
    {
        return prefixedUris.stream()
            .map(uri -> removePrefix(uri, prefixToUri))
            .toList();
    }

    private String removePrefix(final String prefixedUri, final Map<String, String> prefixToUri1)
    {
        final var prefixToUri = Map.of(
            "dc", "http://purl.org/dc/elements/1.1/",
            "obo", "http://purl.obolibrary.org/obo/",
            "owl", "http://www.w3.org/2002/07/owl#",
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "doid", "http://purl.obolibrary.org/obo/doid#",
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
            "skos", "http://www.w3.org/2004/02/skos/core#",
            "terms", "http://purl.org/dc/terms/",
            "oboInOwl", "http://www.geneontology.org/formats/oboInOwl#"
        );

        if (prefixedUri.equals("http://user_neo4j/referencedResource"))
        {
            return prefixedUri;
        }

        if (new UrlValidator().isValid(prefixedUri))
        {
            return prefixedUri;
        }

        final var uriFragments = Arrays.stream(prefixedUri.split(":")).toList();

        if (uriFragments.size() <= 1)
        {
            return prefixedUri;
        }

        final var prefix = uriFragments.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Invalid prefixed uri: %s".formatted(prefixedUri)));

        final var localName = uriFragments.stream().reduce((current, last) -> last)
            .orElseThrow(() -> new IllegalStateException("Invalid prefixed uri: %s".formatted(prefixedUri)));

        return Optional.ofNullable(prefixToUri.get(prefix))
            .map(namespace -> NON_PREFIXED_URI_PATTERN.formatted(namespace, localName))
            .orElseThrow(() -> new ClassNodeException("Invalid prefix: %s.".formatted(prefixedUri), INVALID_PREFIX));
    }
}
