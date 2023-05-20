package com.rdfsonto.prefix.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_PROJECT_ID;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.UriRemoveUniquenessHandler;
import com.rdfsonto.classnode.service.UriUniquenessHandler;
import com.rdfsonto.prefix.database.PrefixNodeRepository;
import com.rdfsonto.prefix.database.PrefixNodeVo;
import com.rdfsonto.project.service.ProjectService;
import com.rdfsonto.rdf4j.KnownPrefix;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class PrefixNodeServiceImpl implements PrefixNodeService
{
    private final PrefixNodeRepository prefixNodeRepository;
    private final ProjectService projectService;
    private final UriUniquenessHandler uriHandler;
    private final UriRemoveUniquenessHandler uriRemoveHandler;

    @Override
    public Optional<PrefixMapping> findAll(final long projectId)
    {
        projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException("Prefixes for project ID %s can not be deleted, because it does not exist.".formatted(projectId),
                INVALID_PROJECT_ID));

        return prefixNodeRepository.findByProjectId(projectId)
            .map(prefix -> prefix.getPrefixes().entrySet().stream()
                .filter(namespace -> !KnownPrefix.UN.getPrefix().equals(namespace.getKey()))
                .map(namespace -> Map.entry(namespace.getKey(), uriRemoveHandler.removeUniqueness(namespace.getValue())))
                .map(this::endNamespaceWithHash)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .map(prefixToUri -> PrefixMapping.builder()
                .withPrefixToUri(prefixToUri)
                .withUriToPrefix(uriToPrefix(prefixToUri))
                .build());
    }

    @Override
    public PrefixMapping save(final long projectId, final Map<String, String> updatePrefixes)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException("Prefixes for project ID %s can not be deleted, because it does not exist.".formatted(projectId),
                INVALID_PROJECT_ID));

        final var projectTag = projectService.getProjectTag(project);

        final var uniquePrefixes = updatePrefixes.entrySet().stream()
            .map(prefix -> Map.entry(prefix.getKey(), KnownPrefix.isKnownPrefix(prefix.getKey())
                ? prefix.getValue() : uriHandler.applyUniqueness(prefix.getValue(), projectTag, true)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final var updatePrefix = prefixNodeRepository.findByProjectId(projectId)
            .orElse(PrefixNodeVo.builder()
                .withProjectId(projectId)
                .build())
            .toBuilder()
            .withPrefixes(uniquePrefixes)
            .build();

        prefixNodeRepository.deleteByProjectId(projectId);
        prefixNodeRepository.save(updatePrefix);

        return findAll(projectId).orElseThrow(() ->
            new IllegalStateException("Could not find a prefix node for project ID: %s after save.".formatted(projectId)));
    }

    @Override
    public void delete(final long projectId)
    {
        projectService.findById(projectId)
            .orElseThrow(() -> new ClassNodeException("Prefixes for project ID %s can not be deleted, because it does not exist.".formatted(projectId),
                INVALID_PROJECT_ID));

        prefixNodeRepository.deleteByProjectId(projectId);
    }

    private Map.Entry<String, String> endNamespaceWithHash(final Map.Entry<String, String> namespace)
    {
        final var name = namespace.getValue();
        final var unifiedName = name.charAt(name.length() - 1) != '#' ? name + "#" : name;

        return Map.entry(namespace.getKey(), unifiedName);
    }

    private Map<String, String> uriToPrefix(final Map<String, String> prefixToUri)
    {
        return prefixToUri.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
}
