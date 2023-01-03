package com.rdfsonto.classnode.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.rdfsonto.classnode.database.ClassNodeVo;


@Component
class ClassNodeMapper
{
    ClassNode mapToDomain(final ClassNodeVo classNodeVo, final List<ClassNodeVo> incoming, final List<ClassNodeVo> outgoing)
    {
        final var incomingMap =
            incoming != null ? incoming.stream().collect(Collectors.toMap(ClassNodeVo::getId, ClassNodeVo::getRelation)) : null;

        final var outgoingMap =
            outgoing != null ? outgoing.stream().collect(Collectors.toMap(ClassNodeVo::getId, ClassNodeVo::getRelation)) : null;

        return ClassNode.builder()
            .withId(classNodeVo.getId())
            .withUri(classNodeVo.getUri())
            .withProperties(classNodeVo.getProperties())
            .withClassLabels(classNodeVo.getClassLabels())
            .withOutgoingNeighbours(outgoingMap)
            .withIncomingNeighbours(incomingMap)
            .build();
    }
}