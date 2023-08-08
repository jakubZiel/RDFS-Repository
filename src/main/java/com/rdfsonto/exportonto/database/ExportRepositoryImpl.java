package com.rdfsonto.exportonto.database;

import static com.rdfsonto.exportonto.database.ExportRepositoryTemplates.EXPORT_NODES_TO_RDF_STATEMENT;
import static com.rdfsonto.exportonto.database.ExportRepositoryTemplates.EXPORT_RELATIONSHIPS_TO_RDF_STATEMENT;

import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.database.ClassNodeVo;
import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.classnode.service.UriUniquenessHandler;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class ExportRepositoryImpl implements ExportRepository
{
    private final Neo4jClient neo4jClient;
    private final ExportMapper exportMapper;
    private final UriUniquenessHandler uriUniquenessHandler;
    private final UniqueUriIdHandler uniqueUriIdHandler;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;

    @Override
    public List<Statement> exportAttributes(final long projectId, final long userId, final Pageable page)
    {
        final var projectTag = uniqueUriIdHandler.uniquerUriTag(userId, projectId);
        final var projectLabel = uriUniquenessHandler.getClassNodeLabel(projectTag);

        final var nodeIds = classNodeNeo4jDriverRepository.findAllByProject(projectLabel, page).stream()
            .map(ClassNodeVo::getId)
            .toList();

        final var query = EXPORT_NODES_TO_RDF_STATEMENT.formatted(projectLabel, nodeIds);

        return neo4jClient.query(query)
            .fetchAs(Statement.class)
            .mappedBy((typeSystem, record) -> exportMapper.mapToVo(record))
            .all()
            .stream().toList();
    }

    @Override
    public List<Statement> exportRelationships(final long projectId, final long userId, final Pageable page)
    {
        final var projectTag = uniqueUriIdHandler.uniquerUriTag(userId, projectId);
        final var projectLabel = uriUniquenessHandler.getClassNodeLabel(projectTag);

        final var nodeIds = classNodeNeo4jDriverRepository.findAllByProject(projectLabel, page).stream()
            .map(ClassNodeVo::getId)
            .toList();

        final var query = EXPORT_RELATIONSHIPS_TO_RDF_STATEMENT.formatted(projectLabel, nodeIds);

        return neo4jClient.query(query)
            .fetchAs(Statement.class)
            .mappedBy((typeSystem, record) -> exportMapper.mapToVo(record))
            .all()
            .stream().toList();
    }
}
