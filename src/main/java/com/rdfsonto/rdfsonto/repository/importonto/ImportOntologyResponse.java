package com.rdfsonto.rdfsonto.repository.importonto;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.RequiredArgsConstructor;


@Node
@RequiredArgsConstructor

public class ImportOntologyResponse
{
    @Id
    private final String id;
}
