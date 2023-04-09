package com.rdfsonto.classnode.database;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


public interface ClassNodeRepository extends Neo4jRepository<ClassNodeVo, Long>
{
    @Query("""
        MATCH (n:Resource)<-[rel]-(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, type(rel) as relation, id(n) as source
        """)
    List<ClassNodeProjection> findAllIncomingNeighbours(@Param("nodeIds") List<Long> ids);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        CALL apoc.neighbors.tohop(n, "<|>", $maxDistance)
        YIELD node
        RETURN node
        UNION
        MATCH (n:Resource) WHERE id(n) = $nodeId
        RETURN n as node
        """)
    List<ClassNodeProjection> findAllNeighbours(@Param("maxDistance") int maxDistance, @Param("nodeId") long sourceNodeId);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        CALL apoc.neighbors.tohop.count(n, "<|>", $maxDistance)
        YIELD value
        RETURN value
        """)
    int countAllNeighbours(@Param("maxDistance") int maxDistance, @Param("nodeId") long sourceNodeId);

    @Query("""
        MATCH (n:Resource)
        WHERE NOT (n)-[:`http://www.w3.org/2000/01/rdf-schema#subClassOf`]->()
        AND ()-[:`http://www.w3.org/2000/01/rdf-schema#subClassOf`]->(n)
        RETURN n
        """)
    List<ClassNodeVo> findAllHierarchyRoots(List<String> relationships);

    // TODO use properties(n) neo4j
    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        UNWIND keys(n) AS prop
        RETURN prop
        """)
    List<String> getAllNodeProperties(@Param("nodeId") long id);

    // TODO use properties(n) neo4j
    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        UNWIND keys(n) AS prop
        RETURN val: n[prop], key: prop
        """)
    List<Object> getAllNodeValues(@Param("nodeId") long id);

    @Query("""
        CALL db.relationshipTypes()
        YIELD relationshipType
        RETURN relationshipType
        """)
    List<String> findAllRelationshipTypes(String projectTag);

    @Query("""
        CALL db.labels()
        YIELD label
        RETURN label
         """)
    List<String> findAllLabels(String projectTag);

    @Query("""
        CALL db.propertyKeys()
        YIELD propertyKey
        RETURN propertyKey
        """)
    List<String> findAllPropertyKeys(String projectTag);

    @Query("""
        MATCH (n:Resource)
        WHERE n.`$key` = "$value"
        RETURN n
        """)
    List<ClassNodeVo> findAllClassNodesByPropertyValue(@Param("key") String key, @Param("value") String value, @Param("tag") String tag);

    Long countAllByClassLabelsContaining(String projectTag);

    void deleteAllByClassLabels(List<String> classLabels);

    Optional<ClassNodeProjection> findByUri(String uri);

    Optional<ClassNodeProjection> findProjectionById(Long id);
}