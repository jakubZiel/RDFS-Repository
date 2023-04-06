package com.rdfsonto.classnode.database;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@Getter
@Builder(setterPrefix = "with")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RelationshipVo
{
    private final Long relationshipId;
    @EqualsAndHashCode.Include
    private final long sourceId;
    @EqualsAndHashCode.Include
    private final long destinationId;
    @EqualsAndHashCode.Include
    private final String relationship;
}
