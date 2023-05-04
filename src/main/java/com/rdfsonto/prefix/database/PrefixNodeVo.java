package com.rdfsonto.prefix.database;

import java.util.Map;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Node("Prefix")
@Builder(setterPrefix = "with", toBuilder = true)
public class PrefixNodeVo
{
    @Id
    @GeneratedValue
    private Long id;

    private Long projectId;

    @CompositeProperty(prefix = "namespace")
    Map<String, String> prefixes;
}
