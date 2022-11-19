package com.rdfsonto.rdfsonto.repository.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


public interface ProjectRepository extends Neo4jRepository<ProjectNode, Long>
{
    @Query("MATCH (u:User{name: $user})-[:OWNS]->(p:Project) RETURN p")
    List<ProjectNode> findProjectNodesByUser(@Param("user") String user);


    @Query("""
        MATCH (u:User) WHERE id(u) = $userId
        MATCH (p:Project {name: $projectName})<-[:OWNS]-(u)
        RETURN p""")
    Optional<ProjectNode> findProjectByNameAndUserId(@Param("projectName") String projectName, @Param("userId") long userId);
}
