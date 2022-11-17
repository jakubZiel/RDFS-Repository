package com.rdfsonto.rdfsonto.model;

import lombok.Getter;

import org.springframework.data.neo4j.core.schema.*;

import java.util.Map;


@Node("Project")
@Getter
public class ProjectNode
{

    @Id
    @GeneratedValue
    long id;

    @Property("name")
    String projectName;

    @Relationship(type = "HAS_ROOT", direction = Relationship.Direction.OUTGOING)
    ClassNode root;

    @CompositeProperty(prefix = "namespaces", delimiter = "__")
    Map<String, String> namespaces;

    public ProjectNode(String projectName, ClassNode root)
    {
        this.projectName = projectName;
        this.root = root;
    }
}
