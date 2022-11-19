package com.rdfsonto.rdfsonto.repository.user;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import com.rdfsonto.rdfsonto.repository.project.ProjectNode;


public interface UserRepository extends Neo4jRepository<UserNode, Long>
{
    @Query("MATCH (n:User) WHERE n.name = $name RETURN n")
    Optional<UserNode> findByUsername(@Param("name") String userName);
}
