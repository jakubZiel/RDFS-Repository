package com.rdfsonto.classnode.database;

public class ClassNodeNeo4jDriverRepositoryTemplates
{
    static final String MATCH_NODE_TEMPLATE = """
        MATCH (node:Resource) WHERE id(node) = $nodeId
        """;
    static final String CREATE_NODE_TEMPLATE = """
        CREATE (node:Resource{uri: $uri}) RETURN node
        """;
    static final String CLEAR_PROPERTIES_TEMPLATE = """
        node = {uri: $uri}
        """;
    static final String SET_NODE_PROPERTIES_TEMPLATE = """
        MATCH (node) WHERE id(node) = %s
        SET""";
    static final String SET_PROPERTY_TEMPLATE = """
        node.`%s` = $param%s""";
    static final String OUTGOING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)-[rel]->(neighbour:Resource)
        WHERE id(n) IN $nodeIds AND id(neighbour) in $nodeIds
        RETURN neighbour, id(n) AS source, type(rel) AS relation, id(rel) AS relationshipId
        """;
    static final String INCOMING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)<-[rel]-(neighbour:Resource)
        WHERE id(n) IN $nodeIds AND id(neighbour) in $nodeIds
        RETURN neighbour, id(n) AS source, type(rel) AS relation, id(rel) AS relationshipId
        """;

    static final String ALL_OUTGOING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)-[rel]->(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, id(n) AS source, type(rel) AS relation, id(rel) AS relationshipId
        """;
    static final String ALL_INCOMING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)<-[rel]-(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, id(n) AS source, type(rel) AS relation, id(rel) AS relationshipId
        """;

    static final String FIND_ALL_NODE_PROPERTIES_QUERY_TEMPLATE = """
        UNWIND $nodeIds AS nodeId
        MATCH (n:Resource) WHERE id(n) = nodeId
        RETURN id(n) AS id, properties(n) AS properties
        """;

    static final String FIND_ALL_NODE_PROPERTIES_BY_PROJECT_LABEL_QUERY_TEMPLATE = """
        MATCH (node:Resource:`%s`)
        RETURN id(node) AS id, properties(node) AS properties, labels(node) as labels
        """;

    static final String DELETE_ALL_RESOURCE_NODES_WITH_LABEL_TEMPLATE = """
        MATCH (n:Resource:`%s`)
        CALL {
            WITH n
            DETACH DELETE n
            } IN TRANSACTIONS OF 10000 ROWS;
        """;

    static final String ADD_LABEL_TO_ALL_NODES_WITH_ID_IN_NODE_IDS = """
        MATCH (n:Resource) WHERE id(n) in $nodeIds
        SET n:%s
        RETURN n
        """;

    static final String COUNT_NODE_KEY = "count(node)";
    static final String NEIGHBOUR_RECORD_KEY = "neighbour";
    static final String RELATION_RECORD_KEY = "relation";
    static final String RELATIONSHIP_ID_KEY = "relationshipId";
    static final String SOURCE_NODE_ID_RECORD_KEY = "source";
    static final String NODE_KEY = "node";
    static final String NODE_IDS_KEY = "nodeIds";
    static final String NODE_ID_KEY = "nodeId";
    static final String URI_KEY = "uri";
    static final String AND = "AND";
}
