package com.rdfsonto.rdfsonto.repository;

import com.rdfsonto.rdfsonto.model.ProjectNode;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ProjectRepository extends Neo4jRepository<ProjectNode, Long> {

    @Query("MATCH (u:User{name: $user})-[:OWNS]->(p:Project) RETURN p")
    Collection<ProjectNode> getProjectNodeByUser(@Param("user") String user);

    @Query("""
            MATCH (u:User) WHERE id(u) = $userId
            MATCH (p:Project {name: $projectName})<-[:OWNS]-(u)
            RETURN p""")
    ProjectNode findProjectByNameAndUser(@Param("projectName") String projectName, @Param("userId") long userId);
}
