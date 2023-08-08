package com.rdfsonto.exportonto.database;

class ExportRepositoryTemplates
{
    static final String PREDICATE_KEY = "predicate";
    static final String OBJECT_KEY = "object";
    static final String SUBJECT_KEY = "subject";
    static final String IS_LITERAL_KEY = "isLiteral";

    static final String EXPORT_NODES_TO_RDF_STATEMENT = """
        CALL n10s.rdf.export.cypher("MATCH p = (n:Resource:`%s`) WHERE id(n) in %s RETURN p")
        YIELD subject, predicate, object, isLiteral
        RETURN subject, predicate, object, isLiteral
        """;

    static final String EXPORT_RELATIONSHIPS_TO_RDF_STATEMENT = """
        CALL n10s.rdf.export.cypher("MATCH (n:Resource:`%s`)-[r]->() WHERE id(n) in %s RETURN r")
        YIELD subject, predicate, object, isLiteral
        RETURN subject, predicate, object, isLiteral
        """;

    // TODO
    static final String EXPORT_NODES_AND_RELATIONSHIPS_TO_RDF_STATEMENT = """
        CALL n10s.rdf.export.cypher ("WITH `%s` AS nodeIds MATCH (n:Resource) WHERE id(n) in nodeIds  MATCH (n:Resource)-[r]->() WHERE id(n) in nodeIds RETURN n, r")
        YIELD subject, predicate , object
        RETURN  subject, predicate , object
        """;
}
