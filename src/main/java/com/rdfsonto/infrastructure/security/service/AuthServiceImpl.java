package com.rdfsonto.infrastructure.security.service;

import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;
import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
    final AuthKeycloakClient authKeycloakClient;
    final UserService userService;

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
    public boolean validateResourceRights(final Long resourceId, final Long userId, final Class<?> resourceType)
    {
        if (resourceType.equals(ProjectNode.class))
        {
            return validateProjectRights(resourceId, userId);
        }

        throw new NotImplementedException("Handling of resource type: %s is not implemented.".formatted(resourceType.getName()));
    }

    private boolean validateProjectRights(final Long projectId, final Long userId)
    {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        final var principal = auth.getPrincipal();

        if (!(principal instanceof final KeycloakPrincipal<?> keycloakPrincipal))
        {
            return false;
        }

        final var subject = keycloakPrincipal
            .getKeycloakSecurityContext()
            .getToken()
            .getSubject();

        return userService.findById(userId).stream()
            .filter(u -> u.getKeycloakId().equals(subject))
            .map(UserNode::getProjectSet)
            .flatMap(Collection::stream)
            .map(ProjectNode::getId)
            .anyMatch(id -> id.equals(projectId));
    }
}
