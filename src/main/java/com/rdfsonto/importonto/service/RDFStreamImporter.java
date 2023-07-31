package com.rdfsonto.importonto.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;


public class RDFStreamImporter
{
    public Path getProcessedRdfFileForNeo4j(final URL inputURL, final String workspaceDirectory, final String tag, final RDFFormat rdfFormat)
        throws IOException
    {
        final var originalOntologyFile = Path.of(workspaceDirectory + tag + ".input");
        final var processedOntologyFile = Path.of(workspaceDirectory + tag + ".output");

        final var ontologyInputFile = downloadFile(inputURL, originalOntologyFile);

        //final var ontologyInputFile = inputURL.getFile();
        final var downloadedInputStream = new FileInputStream(ontologyInputFile);

        final var inputStream = handleInputStream(downloadedInputStream, inputURL);

        final var outputStream = new FileOutputStream(processedOntologyFile.toFile(), true);
        final var importHandler = new RDFStreamImportHandler(outputStream, rdfFormat, tag);

        final var parser = Rio.createParser(rdfFormat);
        parser.setRDFHandler(importHandler);

        importHandler.start();
        parser.parse(inputStream);
        importHandler.stop();

        return processedOntologyFile;
    }

    private File downloadFile(final URL inputURL, final Path outputFile) throws IOException
    {
        final var readableByteChannel = Channels.newChannel(inputURL.openStream());
        final var fileOutputStream = new FileOutputStream(outputFile.toFile());

        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();

        return outputFile.toFile();
    }

    private InputStream handleInputStream(final FileInputStream fileInputStream, final URL inputFile) throws IOException
    {
        final var extension = FilenameUtils.getExtension(inputFile.getFile());

        if (extension.equalsIgnoreCase("gz"))
        {
            return new GZIPInputStream(fileInputStream);
        }

        return fileInputStream;
    }

    public static void main(String[] args) throws IOException
    {
        final var importer = new RDFStreamImporter();

        importer.getProcessedRdfFileForNeo4j(new URL("file:///home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs/mesh2022.ttl"),
            "/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs",
            "@123@123@", RDFFormat.NTRIPLES);
    }
}
