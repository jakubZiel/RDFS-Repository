package com.rdfsonto.rdfsonto.repository.relationship;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Getter;


@Node
@Getter
public class RelationshipNode
{
    @Id
    @GeneratedValue
    long id;
}
