package com.rdfsonto.rdfsonto.service.security;

import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.rdfsonto.rdfsonto.repository.project.ProjectNode;
import com.rdfsonto.rdfsonto.service.user.UserService;

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

        return false;
    }

    private boolean validateProjectRights(final Long projectId, final Long userId)
    {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        final var principal = auth.getPrincipal();

        if (!(principal instanceof final KeycloakPrincipal<?> keycloakPrincipal))
        {
            return false;
        }

        final var user = userService.findById(userId);

        if (user.isEmpty())
        {
            return false;
        }

        final var validUser = user.get();

        final var subject = keycloakPrincipal
            .getKeycloakSecurityContext()
            .getToken()
            .getSubject();

        if (!validUser.getKeycloakId().equals(subject))
        {
            return false;
        }

        final var requestedProject = validUser.getProjectSet().stream()
            .filter(project -> project.getId().equals(projectId))
            .findFirst();

        return requestedProject.isPresent();
    }
}
