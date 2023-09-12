package com.rdfsonto.user.database;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;


public interface UserRepository extends Neo4jRepository<UserNode, Long>
{
    Optional<UserNode> findByUsername(final String username);

    Optional<UserNode> findByKeycloakId(final String keycloakId);
}
