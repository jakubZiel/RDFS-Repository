package com.rdfsonto.elastic.model;

import lombok.Builder;


@Builder(setterPrefix = "with")
public class IndexingResult
{
    private final boolean success;
}
