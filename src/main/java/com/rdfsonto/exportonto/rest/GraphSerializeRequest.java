package com.rdfsonto.exportonto.rest;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder(setterPrefix = "with")
public class GraphSerializeRequest
{
    private final String cypher;
    private final String format;
}
