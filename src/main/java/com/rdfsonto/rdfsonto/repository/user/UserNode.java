package com.rdfsonto.rdfsonto.repository.user;

import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.rdfsonto.rdfsonto.repository.project.ProjectNode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@Node("User")
@Setter
@Getter
@RequiredArgsConstructor
public class UserNode
{
    @Id
    @GeneratedValue
    private final Long id;

    @Property("name")
    final String username;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @Relationship(type = "OWNER", direction = Relationship.Direction.OUTGOING)
    Set<ProjectNode> projectSet;
}


