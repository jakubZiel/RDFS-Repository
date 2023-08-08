package com.rdfsonto.importonto.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.database.ClassNodeProjection;
import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.ClassNodeService;
import com.rdfsonto.classnode.service.UriUniquenessHandler;
import com.rdfsonto.project.service.ProjectService;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class ReferencedResourceHandler
{
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ClassNodeService classNodeService;
    private final ProjectService projectService;
    private final UriUniquenessHandler uriUniquenessHandler;

    List<ClassNode> findAndLabelReferencedResources(final Long projectId)
    {
        final var projectTag = projectService.findById(projectId)
            .map(projectService::getProjectTag)
            .orElseThrow(() -> new IllegalStateException("Could not find a project."));

        final var referencedResourceIds = classNodeRepository.findAllDetachedReferencedResources(projectTag).stream()
            .map(ClassNodeProjection::getId)
            .toList();

        final var projectLabel = uriUniquenessHandler.getClassNodeLabel(projectTag);

        classNodeNeo4jDriverRepository.batchAddLabel(referencedResourceIds, projectLabel);

        return classNodeService.findByIdsLight(projectId, referencedResourceIds);
    }
}
