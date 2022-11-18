package com.rdfsonto.rdfsonto.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.neo4j.core.schema.*;

import java.util.Map;


@Node("Project")
@Getter
@Builder(setterPrefix = "with")
@RequiredArgsConstructor
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
