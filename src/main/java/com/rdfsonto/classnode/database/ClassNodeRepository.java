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

    // TODO needs to be cached, and changed to go after all nodes
    /*
    correct way - about 2.2s for 2.3m, and 4.5m relationships
    match (n:Resource:`http://www.user_neo4j.com#@34@35@`)-[r]->()
    return count(type(r)), type(r)
     */
    @Query("""
        CALL db.relationshipTypes()
        YIELD relationshipType
        WHERE relationshipType CONTAINS $projectTag
        RETURN relationshipType
        """)
    List<String> findAllRelationshipTypes(@Param("projectTag") String projectTag);

    @Query("""
        CALL db.labels()
        YIELD label
        WHERE label CONTAINS $projectTag
        RETURN label
         """)
    List<String> findAllLabels(@Param("projectTag") String projectTag);

    // TODO needs to be cached
    /*
    correct way - about 1.5s for 2.3m nodes
    match (n:Resource:`http://www.user_neo4j.com#@34@35@`) with n
    unwind keys(n) as prop
    return count(prop), prop
     */
    @Query("""
        MATCH(n:Resource)
        WITH DISTINCT(keys(n)) as key_sets
        UNWIND (key_sets) as keys
        return DISTINCT(keys) as key
        """)
    List<String> findAllPropertyKeys(@Param("label") String label);

    @Query("""
        CALL db.propertyKeys() YIELD propertyKey
        WHERE propertyKey CONTAINS $label
        RETURN propertyKey
        UNION
        RETURN "uri" AS propertyKey
        """)
    List<String> findAllPropertyKeysFast(@Param("label") String label);

    @Query("""
        MATCH (n:Resource)
        WHERE n.`$key` = "$value"
        RETURN n
        """)
    List<ClassNodeVo> findAllClassNodesByPropertyValue(@Param("key") String key, @Param("value") String value, @Param("tag") String tag);

    @Query("""
        MATCH (n:Resource)
        WHERE size(labels(n)) = 1 and n.uri CONTAINS $projectTag
        RETURN n
        """)
    List<ClassNodeProjection> findAllDetachedReferencedResources(@Param("projectTag") final String projectTag);

    Long countAllByClassLabelsContaining(String projectTag);

    Optional<ClassNodeProjection> findByUri(String uri);

    Optional<ClassNodeProjection> findProjectionById(Long id);

    List<ClassNodePropertiesProjection> findAllByIdIn(final List<Long> ids);
}