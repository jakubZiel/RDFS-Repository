package com.rdfsonto.importonto.service;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
class RDFStreamImportHandler extends AbstractRDFHandler implements RDFHandler
{
    private static final String USER_NAMESPACE = "http://www.user_neo4j.com#";

    @Getter
    private final Set<IRI> declaredProperties = new HashSet<>();

    @Getter
    private final Set<IRI> undeclaredProperties = new HashSet<>();

    private final RDFWriter rdfWriter;
    private final String tag;
    private long statementCounter;

    public RDFStreamImportHandler(final OutputStream fileOutputStream, final RDFFormat rdfFormat, final String projectTag)
    {
        rdfWriter = Rio.createWriter(rdfFormat, fileOutputStream);
        tag = projectTag;
        statementCounter = 0;
    }

    @Override
    public void handleNamespace(final String prefix, final String uri) throws RDFHandlerException
    {
        rdfWriter.handleNamespace(prefix, uri);
    }

    @Override
    public void handleStatement(final Statement statement) throws RDFHandlerException
    {
        if (statement.getPredicate().equals(RDF.TYPE) && statement.getObject().equals(OWL.OBJECTPROPERTY))
        {
            declaredProperties.add((IRI) statement.getSubject());
        }

        final var taggedSubject = handleSubject(statement.getSubject(), tag);
        final var taggedStatement = Statements.statement(
            taggedSubject,
            handlePredicate(statement.getPredicate(), tag),
            handleObject(statement, tag),
            null);

        if (statement.getPredicate().equals(RDF.TYPE))
        {
            rdfWriter.handleStatement(Statements.statement(taggedSubject, RDF.TYPE, Values.iri(USER_NAMESPACE, tag), null));
        }
        rdfWriter.handleStatement(taggedStatement);
        statementCounter += 1;

        if (statementCounter % 100_000 == 0)
        {
            log.info("Parsed : {}", statementCounter);
        }
    }

    protected Resource handleSubject(final Resource subject, final String tag)
    {
        if (subject.isBNode())
        {
            return subject;
        }

        if (subject.isIRI())
        {
            final var iri = (IRI) subject;
            final var namespace = iri.getNamespace();
            final var localName = iri.getLocalName();

            return Values.iri(namespace + tag + localName);
        }

        throw new NotImplementedException("Handling of invalid subject: %s".formatted(subject));
    }

    protected IRI handlePredicate(final IRI predicate, final String tag)
    {
        if (declaredProperties.contains(predicate))
        {
            final var namespace = predicate.getNamespace();
            final var localName = predicate.getLocalName();

            return Values.iri(namespace + tag + localName);
        }

        undeclaredProperties.add(predicate);

        return predicate;
    }

    protected Value handleObject(final Statement statement, final String tag)
    {
        final var object = statement.getObject();
        if (object.isBNode() || object.isLiteral())
        {
            return object;
        }

        if (object.isIRI())
        {
            final var iriObject = (IRI) object;

            // Referenced resources will be handled after import.
            final var namespace = iriObject.getNamespace();
            final var localName = iriObject.getLocalName();

            return statement.getPredicate().equals(RDF.TYPE) ? iriObject : Values.iri(namespace + tag + localName);
        }

        throw new NotImplementedException("Handling of invalid object: %s".formatted(object));
    }

    public void start()
    {
        rdfWriter.startRDF();
    }

    public void stop()
    {
        rdfWriter.endRDF();
    }

    public static void main(String[] args) throws IOException
    {
        final var input = Path.of("/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs-test/small-onto.ttl");
        final var output = Path.of("/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs-test/result.ttl");
        final var outBuff = new FileOutputStream(output.toFile());
        final var bufferedOut = new BufferedOutputStream(outBuff);
        final var inputBuff = new FileInputStream(input.toFile());

        final var x = new RDFStreamImportHandler(bufferedOut, RDFFormat.TURTLE, "@123@123@");
        final var parser = Rio.createParser(RDFFormat.TURTLE);

        parser.setRDFHandler(x);

        x.start();
        parser.parse(inputBuff);
        x.stop();
    }
}