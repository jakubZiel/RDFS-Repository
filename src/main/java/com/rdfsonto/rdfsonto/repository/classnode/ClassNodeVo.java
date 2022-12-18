package com.rdfsonto.rdfsonto.repository.classnode;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Node("Resource")
@Getter
@Setter
@Builder(setterPrefix = "with")
public class ClassNodeVo
{
    @Id
    @GeneratedValue
    private Long id;

    @DynamicLabels
    private List<String> classLabels;

    @CompositeProperty
    private Map<String, Object> properties;

    @Relationship(direction = INCOMING)
    private Map<String, List<ClassNodeVo>> neighbours;

    private String relation;
    private String uri;
    private Long source;

    public void setProperties(List<String> props)
    {
        this.properties = new HashMap<>();

        props.stream()
            .filter(prop -> !prop.startsWith("uri"))
            .forEach(prop -> {
                final var keyVal = prop.split(";");
                properties.put(keyVal[0], keyVal[1]);
            });
    }
}
