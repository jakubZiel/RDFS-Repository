package com.rdfsonto.prefix.database;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PrefixNodeRepository extends Neo4jRepository<PrefixNodeVo, Long>
{
    Optional<PrefixNodeVo> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
