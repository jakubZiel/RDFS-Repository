package com.rdfsonto.classnode.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum RelationshipDirection
{

    INCOMING("<"), OUTGOING(">"), ANY("<|>");

    private final String value;

}