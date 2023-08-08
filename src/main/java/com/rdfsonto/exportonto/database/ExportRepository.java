package com.rdfsonto.exportonto.database;

import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.springframework.data.domain.Pageable;


public interface ExportRepository
{
    List<Statement> exportAttributes(long projectId, long userId, Pageable page);

    List<Statement> exportRelationships(long projectId, long userId, Pageable page);
}
