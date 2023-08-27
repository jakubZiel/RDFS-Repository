package com.rdfsonto.exportonto.service;

import java.util.Collections;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;

import com.rdfsonto.classnode.service.UriRemoveUniquenessHandler;
import com.rdfsonto.prefix.service.PrefixMapping;
import com.rdfsonto.prefix.service.PrefixNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public class RDFStreamExportHandler implements RDFHandler
{
    private static final String USER_NAMESPACE = "http://www.user_neo4j.com#";
    private final RDFWriter rdfWriter;
    private final UriRemoveUniquenessHandler uriRemoveUniquenessHandler;
    private final PrefixNodeService prefixNodeService;
    private final long projectId;
    private long statementCounter;

    @Override
    public void startRDF() throws RDFHandlerException
    {
        rdfWriter.startRDF();
        statementCounter = 0L;

        final var prefixMap = prefixNodeService.findAll(projectId)
            .map(PrefixMapping::prefixToUri)
            .orElse(Collections.emptyMap());

        prefixMap.forEach(rdfWriter::handleNamespace);
    }

    @Override
    public void endRDF() throws RDFHandlerException
    {
        rdfWriter.endRDF();
        log.info("Finished parsing : {} statements.", statementCounter);
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

        statementCounter += 1;

        if (statementCounter % 100_000 == 0)
        {
            log.info("Parsed : {} statements.", statementCounter);
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
