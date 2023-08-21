package com.rdfsonto.classnode.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.rdfsonto.classnode.database.RelationshipDirection;
import com.rdfsonto.elastic.service.SearchAfterParams;


public interface ClassNodeService
{
    List<ClassNode> findByIds(long projectId, List<Long> ids);

    List<ClassNode> findByIdsLight(long projectId, List<Long> ids);

    NodeSearchResult findByPropertiesAndLabels(long projectId,
                                               List<String> labels,
                                               List<FilterCondition> filters,
                                               final List<PatternFilter> patterns,
                                               final Pageable pageable,
                                               final SearchAfterParams searchAfterParams);

    List<ClassNode> findByProject(final long projectId, final Pageable page);

    Optional<ClassNode> findById(long projectId, Long id);

    List<ClassNode> findNeighboursByUri(long projectId,
                                        String nodeUri,
                                        int maxDistance,
                                        List<String> allowedRelationships,
                                        RelationshipDirection relationshipDirection);

    List<ClassNode> findNeighbours(long projectId,
                                   long id,
                                   int maxDistance,
                                   List<String> allowedRelationships,
                                   RelationshipDirection relationshipDirection);

    ClassNode save(long projectId, ClassNode node);

    void deleteById(long projectId, long id);

    ProjectNodeMetadata findProjectNodeMetaData(long projectId);
}
