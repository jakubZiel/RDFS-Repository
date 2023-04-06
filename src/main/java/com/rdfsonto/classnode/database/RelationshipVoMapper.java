package com.rdfsonto.classnode.database;

import org.springframework.stereotype.Component;


@Component
public class RelationshipVoMapper
{
    public RelationshipVo mapToVo(final ClassNodeVo nodeLink, final long mainNodeId, final boolean incoming)
    {
        final var start = incoming ? nodeLink.getId() : mainNodeId;
        final var end = incoming ? mainNodeId : nodeLink.getId();

        return RelationshipVo.builder()
            .withRelationship(nodeLink.getRelation())
            .withSourceId(start)
            .withDestinationId(end)
            .withRelationshipId(nodeLink.getRelationshipId())
            .build();
    }
}
