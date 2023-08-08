package com.rdfsonto.classnode.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.rdfsonto.rdf4j.KnownNamespace;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class UriRemoveUniquenessHandler
{
    private static final String URI_KEY = "uri";
    private final UniqueUriIdHandler uniqueUriIdHandler;

    ClassNode removeUniqueness(final ClassNode uniqueUriClassNode)
    {
        final var nonUniqueUri = uniqueUriIdHandler.extractUri(uniqueUriClassNode);
        final var nodeLabels = uniqueUriClassNode.classLabels().stream()
            .filter(label -> !label.startsWith(KnownNamespace.UN.toString()))
            .toList();

        final var nonUniqueLabels = removeUniqueness(nodeLabels);

        final var nonUniqueProperties = Optional.ofNullable(uniqueUriClassNode.properties())
            .map(props -> props.entrySet().stream()
                .filter(property -> !property.getKey().equals(URI_KEY))
                .map(this::removeUniqueness)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);

        final var nonUniqueIncoming = removeUniqueness(uniqueUriClassNode.incomingNeighbours());
        final var nonUniqueOutgoing = removeUniqueness(uniqueUriClassNode.outgoingNeighbours());

        return uniqueUriClassNode.toBuilder()
            .withUri(nonUniqueUri)
            .withClassLabels(nonUniqueLabels)
            .withProperties(nonUniqueProperties)
            .withIncomingNeighbours(nonUniqueIncoming)
            .withOutgoingNeighbours(nonUniqueOutgoing)
            .build();
    }

    ProjectNodeMetadata removeUniqueness(final ProjectNodeMetadata uniqueProjectNodeMetadata)
    {
        final var nonUniqueLabels = uniqueProjectNodeMetadata.nodeLabels().stream().map(this::removeUniqueness).toList();
        final var nonUniqueProperties = uniqueProjectNodeMetadata.propertyKeys().stream().map(this::removeUniqueness).toList();
        final var nonUniqueRelationships = uniqueProjectNodeMetadata.relationshipTypes().stream().map(this::removeUniqueness).toList();

        return ProjectNodeMetadata.builder()
            .withNodeLabels(nonUniqueLabels)
            .withPropertyKeys(nonUniqueProperties)
            .withRelationshipTypes(nonUniqueRelationships)
            .build();
    }

    private Map<Long, List<String>> removeUniqueness(final Map<Long, List<String>> neighbourhood)
    {
        return Optional.ofNullable(neighbourhood).map(nonNullNeighbourhood -> nonNullNeighbourhood.entrySet().stream()
                .map(neighbourRelationships -> Map.entry(neighbourRelationships.getKey(), removeUniqueness(neighbourRelationships.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(null);
    }

    private List<String> removeUniqueness(final List<String> uniqueUris)
    {
        return uniqueUris.stream().map(this::removeUniqueness).toList();
    }

    private Map.Entry<String, Object> removeUniqueness(final Map.Entry<String, Object> propertiesEntry)
    {
        final var uniqueUri = propertiesEntry.getKey();

        final var uriFragments = Arrays.stream(uniqueUri.split("#")).toList();
        final var namespaceUrl = uriFragments.stream().findFirst()
            .map(uri -> uri + "#")
            .orElseThrow(() -> new IllegalStateException("Invalid uri %s.".formatted(uniqueUri)));

        return Map.entry(removeUniqueness(propertiesEntry.getKey()), propertiesEntry.getValue());
    }

    public String removeUniqueness(final String uniqueUri)
    {
        return uniqueUriIdHandler.removeUniqueness(uniqueUri);
    }


}
