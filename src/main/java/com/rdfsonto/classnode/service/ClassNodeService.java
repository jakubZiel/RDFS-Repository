package com.rdfsonto.classnode.service;

import java.util.List;
import java.util.Optional;


public interface ClassNodeService
{
    List<ClassNode> findByIds(long projectId, List<Long> ids);

    List<ClassNode> findByPropertiesAndLabels(long projectId, List<String> labels, List<FilterCondition> filters);

    Optional<ClassNode> findById(long projectId, Long id);

    List<ClassNode> findNeighboursByUri(long projectId, String nodeUri, int maxDistance, List<String> allowedRelationships);

    List<ClassNode> findNeighbours(long projectId, long id, int maxDistance, List<String> allowedRelationships);

    ClassNode save(long projectId, ClassNode node);

    void deleteById(long id);

    ProjectNodeMetadata findProjectNodeMetaData(long projectId);
}
