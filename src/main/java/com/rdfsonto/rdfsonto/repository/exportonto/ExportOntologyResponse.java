package com.rdfsonto.rdfsonto.repository.exportonto;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.RequiredArgsConstructor;


@Node
@RequiredArgsConstructor
public class ExportOntologyResponse
{
    @Id
    private final String id;
}
