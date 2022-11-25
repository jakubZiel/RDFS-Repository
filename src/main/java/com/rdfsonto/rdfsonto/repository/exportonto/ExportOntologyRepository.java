package com.rdfsonto.rdfsonto.repository.exportonto;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;


public interface ExportOntologyRepository extends Neo4jRepository<ExportOntologyResponse, String>
{
    @Query("export query")
    ExportOntologyResponse exportOntology();
}
