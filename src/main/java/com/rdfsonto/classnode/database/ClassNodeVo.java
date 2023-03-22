package com.rdfsonto.classnode.database;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Node("Resource")
@Builder(setterPrefix = "with")
public class ClassNodeVo
{
    @Id
    @GeneratedValue
    private Long id;

    @DynamicLabels
    private List<String> classLabels;

    //TODO check if it works
    private Map<String, Object> properties;

    @Relationship(direction = INCOMING)
    private Map<String, List<ClassNodeVo>> neighbours;

    private String relation;
    private String uri;
    private Long source;
}
