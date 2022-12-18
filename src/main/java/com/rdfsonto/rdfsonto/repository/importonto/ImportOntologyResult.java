package com.rdfsonto.rdfsonto.repository.importonto;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Node("ImportOntologyResult")
@Getter
@RequiredArgsConstructor
public class ImportOntologyResult
{
    @Id
    private final String id;
    private final String terminationStatus;
    private final Long triplesLoaded;
    private final Long triplesParsed;
    private final String extraInfo;
}
