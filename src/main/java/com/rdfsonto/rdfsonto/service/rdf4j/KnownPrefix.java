package com.rdfsonto.rdfsonto.service.rdf4j;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;


@AllArgsConstructor
public enum KnownPrefix
{

    RDF("rdf"),
    OWL("owl"),
    XML("xml"),
    XSD("xsd"),
    RDFS("rdfs"),
    UN("un");

    @Getter
    private final String prefix;

    public static boolean isKnownPrefix(final String prefix)
    {
        return Arrays.stream(values())
            .anyMatch(knownPrefix -> prefix.equals(knownPrefix.getPrefix()));
    }

    @Override
    public String toString()
    {
        return prefix;
    }
}
