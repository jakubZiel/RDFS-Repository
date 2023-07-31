package com.rdfsonto.importonto.service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import com.rdfsonto.rdf4j.RDFInputOutput;

import lombok.NoArgsConstructor;


@NoArgsConstructor
public class RDFImporter extends RDFInputOutput
{
    private static final String REFERENCED_RESOURCE = "http://user_neo4j/referencedResource";

    public void prepareRDFFileToMergeIntoNeo4j(final URL inputURL, final Path outputFile, final String tag, final RDFFormat rdfFormat)
        throws IOException
    {
        outModel = new ModelBuilder().build();
        downloadFile(inputURL, outputFile);
        loadModel(outputFile, rdfFormat);
        saveMergeReadyModel(outputFile, rdfFormat, tag);
    }

    public Set<Namespace> getLoadedNamespaces()
    {
        return outModel.getNamespaces();
    }

    private void downloadFile(final URL inputURL, final Path outputFile) throws IOException
    {
        final var readableByteChannel = Channels.newChannel(inputURL.openStream());
        final var fileOutputStream = new FileOutputStream(outputFile.toString());
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();
    }

    private void saveMergeReadyModel(final Path outputFile, final RDFFormat rdfFormat, final String tag)
        throws FileNotFoundException
    {
        model.setNamespace(USER_NAMESPACE_PREFIX, USER_NAMESPACE);

        model.forEach(statement -> {
                final var taggedStatement = tagStatement(statement, tag);
                outModel.add(taggedStatement);
            }
        );
        applyUserLabel(tag);

        final var x = outModel.stream()
            .sorted(Comparator.comparing(o -> o.getSubject().toString()))
            .toList();

        final var name = outputFile.toString();
        final var output = new FileOutputStream(name);

        Rio.write(x, output, rdfFormat);
    }

    private void applyUserLabel(String label)
    {
        Set<Resource> set = new HashSet<>(outModel.subjects());
        set.forEach(sub -> outModel.add(sub, RDF.TYPE, Values.iri(USER_NAMESPACE, label)));
    }

    protected Statement tagStatement(Statement originalStatement, final String tag)
    {
        if (!(validate(originalStatement.getSubject()) && validate(originalStatement.getObject())))
        {
            throw new NotImplementedException("Handling of Triple is not implemented");
        }

        final var subject = originalStatement.getSubject();
        final var predicate = originalStatement.getPredicate();

        final var sub = handleSubject(subject, tag);
        final var pred = handlePredicate(predicate, tag);
        final var obj = handleObject(originalStatement, tag);

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

            return Values.iri(namespace + tag + localName);
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

            return Values.iri(namespace + tag + localName);
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

            if (!model.subjects().contains(iriObject) && !statement.getPredicate().equals(RDF.TYPE))
            {
                addReferenceIndicator(iriObject);
            }

            final var namespace = iriObject.getNamespace();
            final var localName = iriObject.getLocalName();

            return model.subjects().contains(iriObject) ? Values.iri(namespace + tag + localName) : iriObject;
        }

        throw new NotImplementedException("Handling of invalid object: %s".formatted(object));
    }

    void addReferenceIndicator(final IRI referencedValue)
    {
        outModel.add(referencedValue, RDF.TYPE, Values.iri(REFERENCED_RESOURCE));
    }

    public static void main(String[] args) throws IOException
    {
        final RDFImporter d = new RDFImporter();

        d.prepareRDFFileToMergeIntoNeo4j(
            new URL("file:/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs-test/HumanDO.txt"),
            Paths.get("/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs/vw2.owl"),
            "@123@123@",
            RDFFormat.RDFXML);
    }
}
