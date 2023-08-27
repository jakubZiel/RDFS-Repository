package com.rdfsonto.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;

import co.elastic.clients.util.Pair;


@ExtendWith(SoftAssertionsExtension.class)
public class CompareRdfFileTest
{
    @Test
    void testCompareRdfFiles(final SoftAssertions softly) throws IOException
    {
        // given
        final var file1 = new FileInputStream(Path.of("/home/jzielins/Pobrane/obo_onto_3(1)").toFile());
        final var file2 = new FileInputStream(Path.of("/home/jzielins/Pobrane/HumanDO.txt").toFile());
        final var fileFormat = RDFFormat.RDFXML;

        final var model1 = sortedStatement(Rio.parse(file1, fileFormat));
        final var model2 = sortedStatement(Rio.parse(file2, fileFormat));

        // when
        final var pairedStatements = Streams.zip(model1.stream(), model2.stream(), Pair::of).toList();
        pairedStatements.forEach(pair -> softly.assertThat(pair.key().getObject()).isEqualTo(pair.value().getObject()));

        pairedStatements.forEach(pair -> {
            if (!pair.key().getPredicate().equals(pair.value().getPredicate()))
            {
                System.out.println("Failed pair beg");
                System.err.println(pair.key());
                System.err.println(pair.value());
                System.out.println("Failed pair end");
            }
        });

        // then

        file1.close();
        file2.close();
    }

    private List<Statement> sortedStatement(final Model model)
    {
        return model.stream().sorted(new Comparator<Statement>()
        {
            @Override
            public int compare(final Statement o1, final Statement o2)
            {
                final int compareObjects = o1.getObject().toString().compareTo(o2.getObject().toString());
                return compareObjects != 0 ? compareObjects : o1.getPredicate().toString().compareTo(o2.getPredicate().toString());
            }
        }).toList();
    }

}
