package com.rdfsonto.project.database;

import java.util.Map;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Node("Project")
@Setter
@Getter
@EqualsAndHashCode
@Builder(setterPrefix = "with")
public class ProjectNode
{
    @Id
    @GeneratedValue
    private final Long id;

    private Long ownerId;

    @Property("name")
    @EqualsAndHashCode.Exclude
    private final String projectName;

    @EqualsAndHashCode.Exclude
    @CompositeProperty(prefix = "namespaces", delimiter = "__")
    private Map<String, String> namespaces;
}
