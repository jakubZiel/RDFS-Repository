package com.rdfsonto.importonto.service;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;


public class PossiblePropertyDeclarationTypes
{
    public static final Set<IRI> DECLARATIONS = Set.of(
        RDF.PROPERTY,
        OWL.ANNOTATIONPROPERTY,
        OWL.ASYMMETRICPROPERTY,
        OWL.DATATYPEPROPERTY,
        OWL.DEPRECATEDPROPERTY,
        OWL.FUNCTIONALPROPERTY,
        OWL.INVERSEFUNCTIONALPROPERTY,
        OWL.OBJECTPROPERTY,
        OWL.ONTOLOGYPROPERTY,
        OWL.REFLEXIVEPROPERTY,
        OWL.IRREFLEXIVEPROPERTY,
        OWL.SYMMETRICPROPERTY,
        OWL.TRANSITIVEPROPERTY
    );
}