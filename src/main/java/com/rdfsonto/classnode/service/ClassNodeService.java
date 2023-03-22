package com.rdfsonto.classnode.service;

import java.util.List;
import java.util.Optional;


public interface ClassNodeService
{
    List<ClassNode> findByIds(List<Long> ids);

    List<ClassNode> findByPropertyValue(long projectId, String propertyKey, String value);

    List<ClassNode> findByPropertiesAndLabels(final long projectId, final List<String> labels, final List<FilterCondition> filters);

    Optional<ClassNode> findById(Long id);

    List<ClassNode> findNeighboursByUri(String nodeUri, long projectId, int maxDistance, List<String> allowedRelationships);

    List<ClassNode> findNeighbours(long id, int maxDistance, List<String> allowedRelationships);

    ClassNode save(ClassNode node, long projectId);

    void deleteById(long id);

    void deleteAll(long projectId);

    ProjectNodeMetadata findProjectNodeMetaData(long projectId);
}
