package com.rdfsonto.rdfsonto.repository.importonto;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;


public interface ImportOntologyRepository extends Neo4jRepository<ImportOntologyResult, String>
{
    @Query("""
        CALL n10s.rdf.import.fetch("file://$path", "$rdfFormat")
        """)
    ImportOntologyResult importOntology(final String path, final String rdfFormat);
}
