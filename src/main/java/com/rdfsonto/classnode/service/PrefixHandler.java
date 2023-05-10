package com.rdfsonto.classnode.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.rdfsonto.prefix.service.PrefixMapping;
import com.rdfsonto.prefix.service.PrefixNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
class PrefixHandler
{
    private static final String PREFIX_PATTERN = "%s:%s";

    private final PrefixNodeService prefixNodeService;

    List<ClassNode> applyPrefix(final List<ClassNode> classNodes, final long projectId)
    {
        final var prefixMapping = prefixNodeService.findAll(projectId).orElse(null);

        if (prefixMapping == null)
        {
            return classNodes;
        }

        return classNodes.stream().map(classNode -> applyPrefix(classNode, prefixMapping)).toList();
    }

    ProjectNodeMetadata applyPrefix(final ProjectNodeMetadata nonPrefixedMetadata, final long projectId)
    {
        final var uriToPrefix = prefixNodeService.findAll(projectId).map(PrefixMapping::uriToPrefix).orElse(null);

        if (uriToPrefix == null)
        {
            return nonPrefixedMetadata;
        }

        final var prefixedLabels = applyPrefix(nonPrefixedMetadata.nodeLabels(), uriToPrefix);
        final var prefixedProperties = applyPrefix(nonPrefixedMetadata.propertyKeys(), uriToPrefix);
        final var prefixedRelationships = applyPrefix(nonPrefixedMetadata.relationshipTypes(), uriToPrefix);

        return ProjectNodeMetadata.builder()
            .withNodeLabels(prefixedLabels)
            .withPropertyKeys(prefixedProperties)
            .withRelationshipTypes(prefixedRelationships)
            .build();
    }

    ClassNode applyPrefix(final ClassNode classNode, final long projectId)
    {
        final var prefixMapping = prefixNodeService.findAll(projectId).orElse(null);
        return applyPrefix(classNode, prefixMapping);
    }

    private ClassNode applyPrefix(final ClassNode classNode, final PrefixMapping prefixMapping)
    {
        if (prefixMapping == null)
        {
            return classNode;
        }

        final var uriToPrefix = prefixMapping.uriToPrefix();

        final var prefixedUri = applyPrefix(classNode.uri(), uriToPrefix);
        final var prefixedLabels = applyPrefix(classNode.classLabels(), uriToPrefix);
        final var prefixedProperties = applyPrefix(classNode.properties(), uriToPrefix);
        final var prefixedIncoming = applyPrefix(classNode.incomingNeighbours(), prefixMapping);
        final var prefixedOutgoing = applyPrefix(classNode.outgoingNeighbours(), prefixMapping);

        return classNode.toBuilder()
            .withUri(prefixedUri)
            .withClassLabels(prefixedLabels)
            .withProperties(prefixedProperties)
            .withIncomingNeighbours(prefixedIncoming)
            .withOutgoingNeighbours(prefixedOutgoing)
            .build();
    }

    private Map<Long, List<String>> applyPrefix(final Map<Long, List<String>> neighbourhood, final PrefixMapping prefixMapping)
    {
        final var uriToPrefix = prefixMapping.uriToPrefix();
        return Optional.ofNullable(neighbourhood).map(nonNulLNeighbourhood -> nonNulLNeighbourhood.entrySet().stream()
                .map(neighbourRelationships -> Map.entry(neighbourRelationships.getKey(), applyPrefix(neighbourRelationships.getValue(), uriToPrefix)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);
    }

    private Map<String, Object> applyPrefix(final Map<String, Object> properties, final Map<String, String> uriToPrefix)
    {
        return Optional.ofNullable(properties).map(nonNullProperties -> nonNullProperties.entrySet().stream()
                .map(property -> Map.entry(applyPrefix(property.getKey(), uriToPrefix), property.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);
    }

    private List<String> applyPrefix(final List<String> nonPrefixedUris, final Map<String, String> uriToPrefix)
    {
        return nonPrefixedUris.stream()
            .map(uri -> applyPrefix(uri, uriToPrefix))
            .toList();
    }

    private String applyPrefix(final String nonPrefixedUri, final Map<String, String> uriToPrefix)
    {
        final var uriFragments = Arrays.stream(nonPrefixedUri.split("#")).toList();

        final var namespaceUrl = uriFragments.stream().findFirst()
            .map(uri -> uri + "#")
            .orElseThrow(() -> new IllegalStateException("Invalid uri: %s".formatted(nonPrefixedUri)));

        final var localName = uriFragments.stream().reduce((current, last) -> last)
            .orElseThrow(() -> new IllegalStateException("Invalid uri: %s".formatted(nonPrefixedUri)));

        return Optional.ofNullable(uriToPrefix.get(namespaceUrl))
            .map(prefix -> PREFIX_PATTERN.formatted(prefix, localName))
            .orElse(nonPrefixedUri);
    }
}
