package com.rdfsonto.rdfsonto.repository.project;

import java.util.Map;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Node("Project")
@Setter
@Getter
@Builder(setterPrefix = "with")
public class ProjectNode
{
    @Id
    @GeneratedValue
    private final Long id;

    @Property("name")
    private final String projectName;

    private final Long ownerId;

    @CompositeProperty(prefix = "namespaces", delimiter = "__")
    private Map<String, String> namespaces;
}
