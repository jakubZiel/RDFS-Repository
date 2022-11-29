package com.rdfsonto.rdfsonto.repository.importonto;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


public interface ImportOntologyRepository extends Neo4jRepository<ImportOntologyResult, String>
{
    @Query("""
        CALL n10s.rdf.import.fetch($path, $rdfFormat) YIELD terminationStatus, triplesLoaded, triplesParsed, extraInfo
        MATCH (n:ImportOntologyResult)
        return n, terminationStatus, triplesLoaded, triplesParsed, extraInfo
        """)
    ImportOntologyResult importOntology(@Param("path") String path, @Param("rdfFormat") String rdfFormat);
}
