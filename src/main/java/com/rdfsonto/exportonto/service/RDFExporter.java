package com.rdfsonto.exportonto.service;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdf4j.RDFInputOutput;


@Component
public class RDFExporter extends RDFInputOutput
{

    private static final Set<String> KNOWN_NAMESPACES = Set.of(
        "http://www.w3.org/2002/07/owl#",
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "http://www.w3.org/2000/01/rdf-schema#",
        "http://www.user_neo4j.com#",
        "http://www.w3.org/XML/1998/namespace");

    public File prepareRdfFileForExport(final Path inputFile, final String tag, final RDFFormat rdfFormat, final Map<String, String> namespaces)
        throws IOException
    {
        outModel = new ModelBuilder().build();
        loadModel(inputFile, TURTLE);

        final var outputFilePath = Paths.get(generateFileName(inputFile.toAbsolutePath().toString(), "-exp"));
        saveExportReadyModel(outputFilePath, rdfFormat, tag, namespaces);

        return outputFilePath.toFile();
    }

    private void saveExportReadyModel(final Path outputFile, final RDFFormat rdfFormat, final String tag, final Map<String, String> namespaces)
        throws FileNotFoundException
    {
        namespaces.forEach((key, value) -> outModel.setNamespace(key, value));

        knownNamespaces.clear();
        knownNamespaces.addAll(KNOWN_NAMESPACES);

        model.forEach(statement -> {
            final var untaggedStatement = untagStatement(statement, tag);
            if (untaggedStatement == null)
            {
                return;
            }
            outModel.add(untaggedStatement);
        });

        removeUserLabel();
        outModel.removeNamespace(USER_NAMESPACE_PREFIX);

        final var output = new FileOutputStream(outputFile.toString());
        Rio.write(outModel, output, rdfFormat);
    }

    private void removeUserLabel()
    {
        Set<Statement> removeStatements = new HashSet<>();

        outModel.filter(null, RDF.TYPE, null).stream()
            .filter(statement -> ((IRI) statement.getObject()).getNamespace().equals(USER_NAMESPACE))
            .forEach(removeStatements::add);

        removeStatements.forEach(statement -> outModel.remove(statement));
    }

    private Statement untagStatement(final Statement inputStatement, final String tag)
    {
        if (!(validate(inputStatement.getSubject()) && validate(inputStatement.getObject())))
        {
            return inputStatement;
        }

        final var subject = (IRI) inputStatement.getSubject();
        final var predicate = inputStatement.getPredicate();
        final var object = inputStatement.getObject();

        final var sub = handleSubject(subject, tag);
        final var pred = handlePredicate(predicate, tag);
        final var obj = handleObject(object, tag);

        return Statements.statement(sub, pred, obj, null);
    }

    @Override
    protected IRI handleSubject(final IRI subject, final String tag)
    {
        if (!knownNamespaces.contains(subject.getNamespace()))
        {
            final var index = subject.getNamespace().lastIndexOf(tag);
            final var untaggedSub = subject.getNamespace().substring(0, index);
            return Values.iri(untaggedSub + "#", subject.getLocalName());
        }
        return subject;
    }

    @Override
    protected IRI handlePredicate(final IRI predicate, final String tag)
    {
        if (!knownNamespaces.contains(predicate.getNamespace()))
        {
            final var index = predicate.getNamespace().lastIndexOf(tag);
            String untaggedPred = predicate.getNamespace().substring(0, index);
            return Values.iri(untaggedPred + "#", predicate.getLocalName());
        }
        return predicate;
    }

    @Override
    protected Value handleObject(final Value object, final String tag)
    {
        if (object.isLiteral())
        {
            return object;
        }
        final var iriObject = (IRI) object;

        if (!knownNamespaces.contains(iriObject.getNamespace()))
        {
            final var index = iriObject.getNamespace().lastIndexOf(tag);
            final var untaggedObject = iriObject.getNamespace().substring(0, index);
            return Values.iri(untaggedObject + "#", iriObject.getLocalName());
        }
        return iriObject;
    }
}

