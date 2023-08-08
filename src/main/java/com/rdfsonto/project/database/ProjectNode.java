package com.rdfsonto.project.database;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rdfsonto.user.database.UserNode;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Node("Project")
@Setter
@Getter
@EqualsAndHashCode
@Builder(setterPrefix = "with", toBuilder = true)
public class ProjectNode
{
    @Id
    @GeneratedValue
    private final Long id;

    private Long ownerId;

    private Long snapshotTime;
    private String snapshotFile;

    @Property("name")
    @EqualsAndHashCode.Exclude
    private final String projectName;

    @JsonIgnore
    @Relationship(type = "OWNER", direction = Relationship.Direction.INCOMING)
    private UserNode owner;
}
