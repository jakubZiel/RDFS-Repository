package com.rdfsonto.classnode.database;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;


@Component
public class ClassNodeVoMapper
{
    ClassNodeVo mapToVo(final Node genericNode, final String relation, final Long sourceNodeId, final Long relationshipId)
    {
        return ClassNodeVo.builder()
            .withClassLabels(Lists.newArrayList(genericNode.labels()))
            .withId(genericNode.id())
            .withRelation(relation)
            .withSource(sourceNodeId)
            .withRelationshipId(relationshipId)
            .build();
    }

    final private static String PROPERTIES_KEY = "properties";
    final private static String NODE_ID_KEY = "id";
    final private static String LABELS_KEY = "labels";
    final private static String URI_PROPERTY = "uri";

    ClassNodeVo mapToVo(final Record record)
    {
        final var properties = record.get(PROPERTIES_KEY).asMap();

        return ClassNodeVo.builder()
            .withUri(properties.get(URI_PROPERTY).toString())
            .withId(record.get(NODE_ID_KEY).asLong())
            .withProperties(record.get(PROPERTIES_KEY).asMap())
            .withClassLabels(record.get(LABELS_KEY).asList(Value::asString))
            .build();
    }
}
