package com.rdfsonto.rdfsonto.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.neo4j.core.schema.*;

import java.util.Set;


@Node("User")
@Getter
@RequiredArgsConstructor
public class UserNode
{
    @Id
    @GeneratedValue
    private final long id;

    @Property("name")
    final String username;

    @Relationship(type = "OWNER", direction = Relationship.Direction.OUTGOING)
    Set<ProjectNode> projectSet;
}


