package com.rdfsonto.rdfsonto.repository.classnode;

import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;


@Component
class ClassNodeVoMapper
{
    ClassNodeVo mapToVo(final Node genericNode, final String relation, final Long sourceNodeId)
    {
        return ClassNodeVo.builder()
            .withClassLabels(Lists.newArrayList(genericNode.labels()))
            .withId(genericNode.id())
            .withRelation(relation)
            .withSource(sourceNodeId)
            .build();
    }
}
