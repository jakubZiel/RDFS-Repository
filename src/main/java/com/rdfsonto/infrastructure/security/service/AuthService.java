package com.rdfsonto.infrastructure.security.service;

import java.util.List;


public interface AuthService
{
    KeycloakUser save(KeycloakUser keycloakUser);

    void validateProjectAccess(Long projectId);

    void validateUserAccess(Long userId);

    void validateNodeAccess(List<Long> nodeId);
}
