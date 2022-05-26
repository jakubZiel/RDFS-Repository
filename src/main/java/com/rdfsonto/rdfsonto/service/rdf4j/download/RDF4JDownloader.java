package com.rdfsonto.rdfsonto.service.rdf4j.download;


import com.rdfsonto.rdfsonto.service.rdf4j.KnownPrefix;
import com.rdfsonto.rdfsonto.service.rdf4j.export.RDF4JInputOutput;
import lombok.val;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class RDF4JDownloader extends RDF4JInputOutput {
    private String tag;
    public RDF4JDownloader(RDFFormat dataFormat) {
        super(dataFormat);
    }

    public void prepareRDFFileToMergeIntoNeo4j(URL inputURL, Path outputFile, String tag) throws IOException {
        outModel = new ModelBuilder().build();
        this.tag = tag;
        downloadFile(inputURL, outputFile);
        loadModel(outputFile);
        saveMergeReadyModel(outputFile);
    }

    private void downloadFile(URL inputURL, Path outputFile) throws IOException {
        ReadableByteChannel readableByteChannel = Channels.newChannel(inputURL.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile.toString());
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();
    }

    private void saveMergeReadyModel(Path outputFile) throws FileNotFoundException, FileSystemException {

        model.setNamespace(USER_NAMESPACE_PREFIX, USER_NAMESPACE);

        model.getNamespaces().forEach(namespace -> {
            if (KnownPrefix.isKnownPrefix(namespace.getPrefix())) {
                outModel.setNamespace(namespace);
                knownNamespaces.add(namespace.getName());
            } else {
                outModel.setNamespace(
                        namespace.getPrefix(),
                        namespace.getName().replaceAll("#", "_" + tag + "#")
                );
            }
        });

        model.forEach(statement -> {
                    val taggedStatement = tagStatement(statement);
                    if (taggedStatement == null)
                        return;
                    outModel.add(taggedStatement);
                }
        );

        applyUserLabel(tag);
        val name = generateFileName(outputFile.toString(), "-out");
        val output = new FileOutputStream(name);
        Rio.write(outModel, output, dataFormat);
    }

    private void applyUserLabel(String label) {
        Set<Resource> set = new HashSet<>(outModel.subjects());
        set.forEach(sub -> outModel.add(sub, RDF.TYPE, Values.iri(USER_NAMESPACE, label)));
    }

    protected Statement tagStatement(Statement originalStatement) {
        if (!(validate(originalStatement.getSubject()) && validate(originalStatement.getObject())))
            return originalStatement;

        val subject = (IRI) originalStatement.getSubject();
        val predicate = originalStatement.getPredicate();
        val object = originalStatement.getObject();

        val sub = handleSubject(subject);
        val pred = handlePredicate(predicate);
        val obj = handleObject(object);

        return Statements.statement(sub, pred, obj, null);
    }

    @Override
    protected IRI handleSubject(IRI subject) {
        return knownNamespaces.contains(subject.getNamespace()) ? subject :
                Values.iri(subject.getNamespace().replaceAll("#", "_" + tag + "#"), subject.getLocalName());
    }

    @Override
    protected IRI handlePredicate(IRI predicate) {
        return knownNamespaces.contains(predicate.getNamespace()) ? predicate :
                Values.iri(predicate.getNamespace().replaceAll("#", "_" + tag + "#"), predicate.getLocalName());
    }

    @Override
    protected Value handleObject(Value object) {
        return object.isLiteral() ? object :
                knownNamespaces.contains(((IRI) object).getNamespace()) ? object :
                        Values.iri(((IRI) object).getNamespace().replaceAll("#", "_" + tag + "#"), ((IRI) object).getLocalName());
    }

    public static void main(String[] args) throws IOException {
        RDF4JDownloader d = new RDF4JDownloader(RDFFormat.TURTLE);

        d.prepareRDFFileToMergeIntoNeo4j(
                new URL("file:/media/jzielins/SD/sem7/PD2/rdfs-onto/src/main/resources/rdfs/movie.owl"),
                Paths.get("/media/jzielins/SD/sem7/PD2/rdfs-onto/src/main/resources/rdfs/movie2.owl"),
                "projekt_123");
    }
}
