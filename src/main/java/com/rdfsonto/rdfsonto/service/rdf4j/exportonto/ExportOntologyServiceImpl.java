package com.rdfsonto.rdfsonto.service.rdf4j.exportonto;

import java.io.BufferedInputStream;
import java.io.File;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.neo4j.driver.internal.shaded.reactor.util.function.Tuple2;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdfsonto.repository.exportonto.ExportOntologyRepository;
import com.rdfsonto.rdfsonto.service.project.ProjectService;
import com.rdfsonto.rdfsonto.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
class ExportOntologyServiceImpl implements ExportOntologyService
{
    final ProjectService projectService;
    final UserService userService;
    final ExportOntologyRepository exportOntologyRepository;

    @Override
    public Pair<File, BufferedInputStream> exportOntology(final Long userId, final String projectName, final RDFFormat rdfFormat)
    {


        return null;
    }
}
