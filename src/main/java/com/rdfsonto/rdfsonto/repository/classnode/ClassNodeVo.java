package com.rdfsonto.rdfsonto.repository.classnode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Builder;
import lombok.Getter;


@Node("Resource")
@Getter
@Builder(setterPrefix = "with")
public class ClassNodeVo
{
    @Id
    @GeneratedValue
    private long id;

    @DynamicLabels
    private List<String> classLabels;

    private Map<String, String> properties;

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
