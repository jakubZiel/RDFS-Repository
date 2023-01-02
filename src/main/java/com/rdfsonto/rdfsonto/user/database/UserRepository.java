package com.rdfsonto.rdfsonto.user.database;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


public interface UserRepository extends Neo4jRepository<UserNode, Long>
{
    @Query("""
        MATCH (n:User)
        WHERE n.name = $name
        RETURN n
        """)
    Optional<UserNode> findByUsername(@Param("name") String userName);
}
