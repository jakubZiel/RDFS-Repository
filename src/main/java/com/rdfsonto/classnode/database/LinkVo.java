package com.rdfsonto.classnode.database;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@Getter
@EqualsAndHashCode
@Builder(setterPrefix = "with")
public class LinkVo
{
    private final long sourceId;
    private final long destinationId;
    private final String relationship;
}
