package com.rdfsonto.elastic.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.FilterCondition;
import com.rdfsonto.elastic.model.IndexingResult;


public interface ElasticSearchClassNodeService
{
    List<ElasticSearchClassNode> search(long userId, long projectId, List<FilterCondition> filters, List<String> labels, Pageable page);

    void save(long userId, long projectId, ClassNode classNode);

    void delete(long userId, long projectId, ClassNode classNode);
}
