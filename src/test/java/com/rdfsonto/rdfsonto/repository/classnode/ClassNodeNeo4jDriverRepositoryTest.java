package com.rdfsonto.rdfsonto.repository.classnode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.testcontainers.containers.Neo4jContainer;


@DataNeo4jTest
class ClassNodeNeo4jDriverRepositoryTest
{
    private static Neo4jContainer<?> neo4jContainer;

    @BeforeAll
    static void init()
    {
    }

    @Test
    void findAllOutgoingNeighbours()
    {
    }
}