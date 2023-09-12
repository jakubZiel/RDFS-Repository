package com.rdfsonto.infrastructure.security.service;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.UNAUTHORIZED_RESOURCE_ACCESS;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.UriUniquenessHandler;
import com.rdfsonto.project.database.ProjectNode;
import com.rdfsonto.user.database.UserNode;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.parser.ParseException;


@Slf4j
@Component
@RequiredArgsConstructor
class AuthServiceImpl implements AuthService
{
    private final AuthKeycloakClient authKeycloakClient;
    private final UserService userService;
    private final UriUniquenessHandler uriUniquenessHandler;
    private final ClassNodeRepository classNodeRepository;

    @Override
    public KeycloakUser save(final KeycloakUser createKeycloakUserRequest)
    {
        try
        {
            return authKeycloakClient.createKeycloakUser(createKeycloakUserRequest);
        }
        catch (final ParseException parseException)
        {
            log.error(parseException.getMessage());
            return null;
        }
    }

    @Override
    public void validateUserAccess(final Long userId)
    {
        final var keycloakId = getKeycloakId().orElse(null);
        userService.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ClassNodeException("Unauthorized access to user with id : %s".formatted(userId), UNAUTHORIZED_RESOURCE_ACCESS));
    }

    @Override
    public void validateNodeAccess(final List<Long> nodeIds)
    {
        final var nodes = classNodeRepository.findAllByIdIn(nodeIds);
        if (nodes.isEmpty())
        {
            return;
        }

        final var dominantNode = nodes.get(0);
        final var inferredProjectId = uriUniquenessHandler.getProjectIdFromLabels(dominantNode.getClassLabels())
            .orElseThrow(() -> new IllegalStateException("Node id : %s without project label.".formatted(nodes.get(0).getId())));

        final var nodesWithDifferentProject = nodes.stream()
            .filter(node -> !inferredProjectId.equals(uriUniquenessHandler.getProjectIdFromLabels(node.getClassLabels()).orElse(null)))
            .findAny();

        if (nodesWithDifferentProject.isPresent())
        {
            throw new ClassNodeException("Unauthorized access to nodes with id : %s".formatted(nodeIds), UNAUTHORIZED_RESOURCE_ACCESS);
        }

        try
        {
            validateProjectAccess(inferredProjectId);
        }
        catch (final ClassNodeException classNodeException)
        {
            throw new ClassNodeException("Unauthorized access to nodes with id : %s".formatted(nodeIds), UNAUTHORIZED_RESOURCE_ACCESS);
        }
    }

    @Override
    public void validateProjectAccess(final Long projectId)
    {
        final var keycloakId = getKeycloakId().orElse(null);

        final var isOwned = userService.findByKeycloakId(keycloakId).stream()
            .map(UserNode::getProjectSet)
            .flatMap(Collection::stream)
            .map(ProjectNode::getId)
            .anyMatch(id -> id.equals(projectId));

        if (!isOwned)
        {
            throw new ClassNodeException("Unauthorized access to project with id : %s".formatted(projectId), UNAUTHORIZED_RESOURCE_ACCESS);
        }
    }

    private Optional<String> getKeycloakId()
    {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        final var principal = auth.getPrincipal();

        if (!(principal instanceof final KeycloakPrincipal<?> keycloakPrincipal))
        {
            return Optional.empty();
        }

        return Optional.of(keycloakPrincipal
            .getKeycloakSecurityContext()
            .getToken()
            .getSubject());
    }
}
