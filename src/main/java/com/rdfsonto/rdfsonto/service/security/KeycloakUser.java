package com.rdfsonto.rdfsonto.service.security;

import lombok.Builder;


@Builder(setterPrefix = "with", toBuilder = true)
public record KeycloakUser(String username, String firstName, String lastName, String email, String password, String keycloakId)
{
}