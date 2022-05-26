package com.rdfsonto.rdfsonto.model;

import lombok.Getter;
import lombok.val;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Node("Resource")
@Getter
public class ClassNode {

    @Id
    @GeneratedValue
    long id;

    @DynamicLabels
    List<String> classLabels;

    @Relationship
    Map<String, ClassNode> neighbours;

    Map<String, String> properties;

    String uri;

    public ClassNode(List<String> classLabels, Map<String, ClassNode> neighbours) {
        this.neighbours = neighbours;
        this.classLabels = classLabels;
    }

    public void setProperties(List<String> props) {

        this.properties = new HashMap<>();

        props.stream()
                .filter(prop -> !prop.startsWith("uri"))
                .forEach(prop -> {
                    val keyVal = prop.split(";");
                    properties.put(keyVal[0], keyVal[1]);
                });
    }
}
