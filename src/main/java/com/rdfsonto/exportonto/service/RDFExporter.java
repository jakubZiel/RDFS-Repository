package com.rdfsonto.exportonto.service;

import static org.eclipse.rdf4j.rio.RDFFormat.RDFXML;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdf4j.KnownNamespace;
import com.rdfsonto.rdf4j.RDFInputOutput;


@Component
public class RDFExporter extends RDFInputOutput
{
    private static final String REFERENCED_RESOURCE = "http://user_neo4j/referencedResource";

    public File prepareRdfFileForExport(final Path inputFile, final String tag, final RDFFormat rdfFormat, final Map<String, String> namespaces)
        throws IOException
    {
        outModel = new ModelBuilder().build();
        loadModel(inputFile, rdfFormat);

        final var outputFilePath = Paths.get(generateFileName(inputFile.toAbsolutePath().toString(), "-exp"));
        saveExportReadyModel(outputFilePath, rdfFormat, tag, namespaces);

        return outputFilePath.toFile();
    }

    private void saveExportReadyModel(final Path outputFile, final RDFFormat rdfFormat, final String tag, final Map<String, String> namespaces)
        throws FileNotFoundException
    {
        namespaces.forEach((key, value) -> outModel.setNamespace(key, value));

        knownNamespaces.clear();
        knownNamespaces.addAll(Arrays.stream(KnownNamespace.values()).map(KnownNamespace::toString).collect(Collectors.toSet()));

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
        outModel.removeIf(statement -> statement.getObject().toString().equals(REFERENCED_RESOURCE));

        final var x = outModel.stream().sorted(new Comparator<Statement>()
        {
            @Override
            public int compare(final Statement o1, final Statement o2)
            {
                return o1.getSubject().toString().compareTo(o2.getSubject().toString());
            }
        }).toList();

        final var newModel = new ModelBuilder().build();
        newModel.addAll(x);
        outModel.getNamespaces().forEach(namespace -> newModel.setNamespace(namespace.getPrefix(), namespace.getName()));

        final var output = new FileOutputStream(outputFile.toString());
        Rio.write(newModel, output, rdfFormat);
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

        final var subject = inputStatement.getSubject();
        final var predicate = inputStatement.getPredicate();

        final var sub = handleSubject(subject, tag);
        final var pred = handlePredicate(predicate, tag);
        final var obj = handleObject(inputStatement, tag);

        return Statements.statement(sub, pred, obj, null);
    }

    @Override
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

            final var untaggedName = localName.replace(tag, "");

            return Values.iri(namespace, untaggedName);
        }

        throw new NotImplementedException("Handling of invalid subject: %s".formatted(subject));
    }

    @Override
    protected IRI handlePredicate(final IRI predicate, final String tag)
    {
        if (model.subjects().contains(predicate))
        {
            final var namespace = predicate.getNamespace();
            final var localName = predicate.getLocalName();

            final var untaggedName = localName.replace(tag, "");

            return Values.iri(namespace, untaggedName);
        }

        return predicate;
    }

    @Override
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

            final var namespace = iriObject.getNamespace();
            final var localName = iriObject.getLocalName();

            final var untaggedName = localName.replace(tag, "");

            return Values.iri(namespace, untaggedName);
        }

        throw new NotImplementedException("Handling of invalid object: %s.".formatted(object));
    }

    public static void main(String[] args) throws IOException
    {
        final var exporter = new RDFExporter();
        final var inputPath = Paths.get("/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs/vw2.owl");

        final var namespaces = Map.of(
            "dc", "http://purl.org/dc/elements/1.1/",
            "obo", "http://purl.obolibrary.org/obo/",
            "owl", "http://www.w3.org/2002/07/owl#",
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "doid", "http://purl.obolibrary.org/obo/doid#",
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
            "skos", "http://www.w3.org/2004/02/skos/core#",
            "terms", "http://purl.org/dc/terms/",
            "oboInOwl", "http://www.geneontology.org/formats/oboInOwl#"
        );

        exporter.prepareRdfFileForExport(inputPath, "@123@123@", RDFXML, namespaces);
    }
}

