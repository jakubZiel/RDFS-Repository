package com.rdfsonto.rdfsonto.service.rdf4j.export;

import com.rdfsonto.rdfsonto.service.rdf4j.KnownPrefix;
import com.rdfsonto.rdfsonto.service.rdf4j.RDF4JInputOutput;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;


public class RDF4JExporter extends RDF4JInputOutput
{
    private String tag;

    public RDF4JExporter(RDFFormat dataFormat)
    {
        super(dataFormat);
    }

    public void prepareRDFFileForExport(Path inputFile, String tag) throws IOException
    {
        outModel = new ModelBuilder().build();
        this.tag = tag;
        loadModel(inputFile);

        saveExportReadyModel(
            Paths.get(generateFileName(
                inputFile.toAbsolutePath().toString(),
                "-exp")));
    }

    private void saveExportReadyModel(Path outputFile) throws FileNotFoundException
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
            final var untaggedStatement = untagStatement(statement);
            if (untaggedStatement == null)
            {
                return;
            }
            outModel.add(untaggedStatement);
        });

        removeUserLabel();
        outModel.removeNamespace(USER_NAMESPACE_PREFIX);

        final var output = new FileOutputStream(outputFile.toString());
        Rio.write(outModel, output, dataFormat);
    }

    private void removeUserLabel()
    {
        Set<Statement> removeStatements = new HashSet<>();

        outModel.filter(null, RDF.TYPE, null).stream()
            .filter(statement -> ((IRI) statement.getObject()).getNamespace().equals(USER_NAMESPACE))
            .forEach(removeStatements::add);

        removeStatements.forEach(statement -> outModel.remove(statement));
    }

    private Statement untagStatement(Statement inputStatement)
    {
        if (!(validate(inputStatement.getSubject()) && validate(inputStatement.getObject())))
        {
            return inputStatement;
        }

        final var subject = (IRI) inputStatement.getSubject();
        final var predicate = inputStatement.getPredicate();
        final var object = inputStatement.getObject();

        final var sub = handleSubject(subject);
        final var pred = handlePredicate(predicate);
        final var obj = handleObject(object);

        return Statements.statement(sub, pred, obj, null);
    }

    @Override
    protected IRI handleSubject(IRI subject)
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
    protected IRI handlePredicate(IRI predicate)
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
    protected Value handleObject(Value object)
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
        RDF4JExporter e = new RDF4JExporter(RDFFormat.TURTLE);

        e.prepareRDFFileForExport(
            Paths.get("/media/jzielins/SD/sem7/PD2/rdfs-onto/src/main/resources/rdfs/movie2-out.owl"),
            "projekt_123"
        );
    }
}

