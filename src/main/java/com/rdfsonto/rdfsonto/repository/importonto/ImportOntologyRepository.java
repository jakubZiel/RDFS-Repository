package com.rdfsonto.rdfsonto.repository.importonto;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;


public interface ImportOntologyRepository extends Neo4jRepository<ImportOntologyResponse, String>
{
    @Query("import query")
    ImportOntologyResponse importOntology();
}
