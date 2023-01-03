package com.rdfsonto.infrastructure.security.service;

import lombok.Builder;


@Builder(setterPrefix = "with", toBuilder = true)
public record KeycloakUser(String username, String firstName, String lastName, String email, String password, String keycloakId)
{
}