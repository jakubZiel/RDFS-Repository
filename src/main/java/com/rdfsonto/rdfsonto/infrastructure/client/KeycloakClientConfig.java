package com.rdfsonto.rdfsonto.infrastructure.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class KeycloakClientConfig
{
    private static final int CONNECTION_SIZE_POOL = 10;
    @Value("${keycloak.auth-server-url}")
    private String AUTH_SERVER_URL;
    @Value("${keycloak.realm}")
    private String REALM;
    @Value("admin-cli")
    private String CLIENT_ID;
    @Value("${custom.keycloak.client.secret}")
    private String CLIENT_SECRET;
    @Value("${custom.keycloak.admin-login}")
    private String ADMIN_USERNAME;
    @Value("${custom.keycloak.admin-password}")
    private String ADMIN_PASSWORD;

    @Bean
    UsersResource keycloakUsersRepository()
    {
        final var keycloak = KeycloakBuilder.builder()
            .serverUrl(AUTH_SERVER_URL)
            .realm(REALM)
            .grantType(OAuth2Constants.PASSWORD)
            .username(ADMIN_USERNAME)
            .password(ADMIN_PASSWORD)
            .clientId(CLIENT_ID)
            .clientSecret(CLIENT_SECRET)
            .resteasyClient(
                new ResteasyClientBuilder()
                    .connectionPoolSize(CONNECTION_SIZE_POOL)
                    .build())
            .build();

        return keycloak
            .realm(REALM)
            .users();
    }
}
