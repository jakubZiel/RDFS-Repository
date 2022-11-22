package com.rdfsonto.rdfsonto.service.security;

public interface AuthService
{
    KeycloakUser save(KeycloakUser keycloakUser);

    boolean validateResourceRights(Long resourceId, Long userId, Class<?> resourceType);
}
