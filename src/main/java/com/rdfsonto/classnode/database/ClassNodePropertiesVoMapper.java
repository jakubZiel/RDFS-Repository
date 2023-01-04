package com.rdfsonto.classnode.database;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.springframework.stereotype.Component;


@Component
public class ClassNodePropertiesVoMapper
{
    final private static String PROPERTIES_KEY = "properties";
    final private static String NODE_ID_KEY = "id";

    public ClassNodePropertiesVo mapToVo(final Record record)
    {
        return ClassNodePropertiesVo.builder()
            .withNodeId(record.get(NODE_ID_KEY).asLong())
            .withProperties(record.get(PROPERTIES_KEY).asMap(Value::toString))
            .build();
    }
}