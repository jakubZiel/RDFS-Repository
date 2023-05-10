package com.rdfsonto.classnode.database;

public class ClassNodeNeo4jDriverRepositoryTemplates
{
    static final String MATCH_NODE_TEMPLATE = """
        MATCH (node:Resource) where id(node) = $nodeId
        """;
    static final String CREATE_NODE_TEMPLATE = """
        CREATE (node:Resource{uri: $uri}) return node
        """;
    static final String CLEAR_PROPERTIES_TEMPLATE = """
        node = {uri: $uri}
        """;
    static final String SET_NODE_PROPERTIES_TEMPLATE = """
        MATCH (node) where id(node) = %s
        SET""";
    static final String SET_PROPERTY_TEMPLATE = """
        node.`%s` = $param%s""";
    static final String OUTGOING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)-[rel]->(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, id(n) as source, type(rel) as relation, id(rel) as relationshipId
        """;
    static final String INCOMING_NEIGHBOURS_QUERY_TEMPLATE = """
        MATCH (n:Resource)<-[rel]-(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, id(n) as source, type(rel) as relation, id(rel) as relationshipId
        """;
    static final String FIND_ALL_NODE_PROPERTIES_QUERY_TEMPLATE = """
        UNWIND $nodeIds AS nodeId
        MATCH (n:Resource) WHERE id(n) = nodeId
        RETURN id(n) as id, properties(n) as properties
        """;

    static final String DELETE_ALL_RESOURCE_NODES_WITH_LABEL_TEMPLATE = """
        MATCH (n:Resource:`%s`) DETACH DELETE n
        """;

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
