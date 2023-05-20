package com.rdfsonto.rdf4j;

import java.util.Arrays;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public enum KnownNamespace
{
    OWL("http://www.w3.org/2002/07/owl#"),
    RDF("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    RDFS("http://www.w3.org/2000/01/rdf-schema#"),
    UN("http://www.user_neo4j.com#"),
    XML("http://www.w3.org/XML/1998/namespace");

    private final String name;

    public static boolean isKnownNamespace(final String namespace)
    {
        return Arrays.stream(values())
            .anyMatch(knownPrefix -> namespace.equals(knownPrefix.toString()));
    }

    @Override
    public String toString()
    {
        return name;
    }
}
