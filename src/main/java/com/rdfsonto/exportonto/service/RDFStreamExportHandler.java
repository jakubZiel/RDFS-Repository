package com.rdfsonto.exportonto.service;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;

import com.rdfsonto.classnode.service.UriRemoveUniquenessHandler;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class RDFStreamExportHandler implements RDFHandler
{
    private static final String USER_NAMESPACE = "http://www.user_neo4j.com#";
    private final RDFWriter rdfWriter;
    private final UriRemoveUniquenessHandler uriRemoveUniquenessHandler;

    @Override
    public void startRDF() throws RDFHandlerException
    {
        rdfWriter.startRDF();
    }

    @Override
    public void endRDF() throws RDFHandlerException
    {
        rdfWriter.endRDF();
    }

    @Override
    public void handleNamespace(final String prefix, final String uri) throws RDFHandlerException
    {

    }

    @Override
    public void handleComment(final String comment) throws RDFHandlerException
    {

    }

    @Override
    public void handleStatement(final Statement statement) throws RDFHandlerException
    {
        final var untaggedStatement = removeUniqueness(statement);

        if (statement.getPredicate().equals(RDF.TYPE))
        {
            final var type = (IRI) statement.getObject();

            if (type.getNamespace().equals(USER_NAMESPACE))
            {
                return;
            }
        }

        rdfWriter.handleStatement(untaggedStatement);
    }

    private Statement removeUniqueness(final Statement uniqueStatement)
    {
        final var subject = uriRemoveUniquenessHandler.removeUniqueness(uniqueStatement.getSubject().toString());
        final var predicate = uriRemoveUniquenessHandler.removeUniqueness(uniqueStatement.getPredicate().toString());

        final var object = uniqueStatement.getObject().isLiteral() || uniqueStatement.getObject().isBNode() ? uniqueStatement.getObject() :
            Values.iri(uriRemoveUniquenessHandler.removeUniqueness((uniqueStatement.getObject()).toString()));

        return Statements.statement(
            Values.iri(subject),
            Values.iri(predicate),
            object,
            null);
    }
}
