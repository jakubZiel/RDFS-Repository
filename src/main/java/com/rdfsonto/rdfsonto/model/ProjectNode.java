package com.rdfsonto.rdfsonto.model;

import java.util.Map;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.Builder;
import lombok.Getter;


@Node("Project")
@Getter
@Builder(setterPrefix = "with")
public class ProjectNode
{
    @Id
    @GeneratedValue
    private final long id;

    @Property("name")
    private final String projectName;

    @Relationship(type = "OWNER", direction = Relationship.Direction.INCOMING)
    private UserNode owner;

    @CompositeProperty(prefix = "namespaces", delimiter = "__")
    private Map<String, String> namespaces;
}
