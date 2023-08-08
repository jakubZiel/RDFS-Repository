package com.rdfsonto.exportonto.database;

import static com.rdfsonto.exportonto.database.ExportRepositoryTemplates.IS_LITERAL_KEY;
import static com.rdfsonto.exportonto.database.ExportRepositoryTemplates.OBJECT_KEY;
import static com.rdfsonto.exportonto.database.ExportRepositoryTemplates.PREDICATE_KEY;
import static com.rdfsonto.exportonto.database.ExportRepositoryTemplates.SUBJECT_KEY;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Component;


@Component
class ExportMapper
{
    Statement mapToVo(final Record neo4jTripleRecord)
    {
        final var subject = neo4jTripleRecord.get(SUBJECT_KEY).asString();
        final var predicate = neo4jTripleRecord.get(PREDICATE_KEY).asString();
        final var object = neo4jTripleRecord.get(OBJECT_KEY).asString();
        final var isLiteral = neo4jTripleRecord.get(IS_LITERAL_KEY).asBoolean();

        return Statements.statement(
            Values.iri(subject),
            Values.iri(predicate),
            isLiteral ? Values.literal(object) : Values.iri(object),
            null);
    }
}
