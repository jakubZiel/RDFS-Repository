package com.rdfsonto.rdfsonto.exportonto.service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
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

import com.rdfsonto.rdfsonto.rdf4j.KnownPrefix;
import com.rdfsonto.rdfsonto.rdf4j.RDFInputOutput;


public class RDFExporter extends RDFInputOutput
{
    public void prepareRDFFileForExport(final Path inputFile, final String tag, final RDFFormat rdfFormat) throws IOException
    {
        outModel = new ModelBuilder().build();
        loadModel(inputFile, rdfFormat);

        saveExportReadyModel(
            Paths.get(generateFileName(inputFile.toAbsolutePath().toString(), "-exp")),
            rdfFormat,
            tag);
    }

    private void saveExportReadyModel(final Path outputFile, final RDFFormat rdfFormat, final String tag) throws FileNotFoundException
    {
        model.getNamespaces().forEach(namespace -> {
            if (KnownPrefix.isKnownPrefix(namespace.getPrefix()))
            {
                outModel.setNamespace(namespace);
                knownNamespaces.add(namespace.getName());
            }
            else
            {
                outModel.setNamespace(
                    namespace.getPrefix(),
                    namespace.getName().substring(0, namespace.getName().lastIndexOf("_" + tag + "#")) + "#"
                );
            }
        });

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
            final var index = subject.getNamespace().lastIndexOf("_" + tag);
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
            final var index = predicate.getNamespace().lastIndexOf("_" + tag);
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
            final var index = iriObject.getNamespace().lastIndexOf("_" + tag);
            final var untaggedObject = iriObject.getNamespace().substring(0, index);
            return Values.iri(untaggedObject + "#", iriObject.getLocalName());
        }
        return iriObject;
    }

    public static void main(String[] args) throws IOException
    {
        RDFExporter e = new RDFExporter();

        e.prepareRDFFileForExport(
            Paths.get("/media/jzielins/SD/sem7/PD2/rdfs-onto/src/main/resources/rdfs/movie2-out.owl"),
            "projekt_123",
            RDFFormat.TURTLE
        );
    }
}

