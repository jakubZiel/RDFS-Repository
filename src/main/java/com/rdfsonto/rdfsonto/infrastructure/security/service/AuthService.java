package com.rdfsonto.rdfsonto.infrastructure.security.service;

public interface AuthService
{
    KeycloakUser save(KeycloakUser keycloakUser);

    boolean validateResourceRights(Long resourceId, Long userId, Class<?> resourceType);
}
