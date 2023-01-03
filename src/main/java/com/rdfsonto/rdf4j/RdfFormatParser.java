package com.rdfsonto.rdf4j;

import org.eclipse.rdf4j.rio.RDFFormat;


public class RdfFormatParser
{
    public static RDFFormat parse(final String rdfFormat)
    {
        return switch (rdfFormat)
            {
                case "RDF/XML" -> RDFFormat.RDFXML;
                case "N-Triples" -> RDFFormat.NTRIPLES;
                case "Turtle" -> RDFFormat.TURTLE;
                case "RDF/JSON" -> RDFFormat.RDFJSON;
                default -> null;
            };
    }
}
