package com.rdfsonto.elastic.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.FilterCondition;


public interface ElasticSearchClassNodeService
{
    List<ElasticSearchClassNode> search(long userId, long projectId, List<FilterCondition> filters, List<String> labels, Pageable page, SearchAfterParams lastPageResult);

    void save(long userId, long projectId, ClassNode classNode);

    void delete(long userId, long projectId, ClassNode classNode);

    void deleteIndex(long userId, long projectId);
}
