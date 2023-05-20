package com.rdfsonto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;


public class Runner
{
    public static void main(String[] args) throws IOException
    {
        final var path = Path.of("/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs-test/HumanDO.txt");

        final var input = new FileInputStream(path.toFile().toString());
        final var m = Rio.parse(input, "", RDFFormat.RDFXML);

        final var subjects = m.subjects().stream().map(Object::toString).collect(Collectors.toSet());

        final var outPath = Path.of("/home/jzielins/Projects/ontology-editor-backend/src/main/resources/rdfs/out-test2.ttl");
        final var newOutFile = new File(outPath.toString());

        final var output = new FileOutputStream(newOutFile.toString());
         Rio.write(m, output, RDFFormat.RDFXML);

    }
}
