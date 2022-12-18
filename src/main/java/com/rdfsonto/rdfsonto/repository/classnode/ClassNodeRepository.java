package com.rdfsonto.rdfsonto.repository.classnode;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


public interface ClassNodeRepository extends Neo4jRepository<ClassNodeVo, Long>
{
    @Query("""
        MATCH (n:Resource)-[relation]->(neighbour:Resource)
        WHERE id(n) = $nodeId
        RETURN neighbour, type(relation) as relation
        """)
    List<ClassNodeVo> findAllOutgoingNeighbours(@Param("nodeId") long id);

    @Query("""
        MATCH (n:Resource)<-[relation]-(neighbour:Resource)
        WHERE id(n) = $nodeId
        RETURN neighbour, type(relation) as relation
        """)
    List<ClassNodeVo> findAllIncomingNeighbours(@Param("nodeId") long id);

    @Query("""
        MATCH (n:Resource)<-[rel]-(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, type(rel) as relation, id(n) as source
        """)
    List<ClassNodeVo> findAllIncomingNeighbours(@Param("nodeIds") List<Long> ids);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        UNWIND keys(n) AS prop
        RETURN prop
        """)
    List<String> getAllNodeProperties(@Param("nodeId") long id);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        UNWIND keys(n) AS prop
        RETURN val: n[prop], key: prop
        """)
    List<Object> getAllNodeValues(@Param("nodeId") long id);

    Long countAllByClassLabelsContaining(String projectTag);
}