package com.rdfsonto.rdfsonto.infrastructure.config.security;

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;


@Configuration
public class SecurityConfigDependencies
{
    @Bean
    KeycloakSpringBootConfigResolver keycloakSpringBootConfigResolver()
    {
        return new KeycloakSpringBootConfigResolver();
    }

    @Bean
    KeycloakAuthenticationProvider keycloakAuthProvider()
    {
        final var authorityMapper = new SimpleAuthorityMapper();

        authorityMapper.setPrefix("ROLE_");
        authorityMapper.setConvertToUpperCase(true);

        final var keycloakAuthProvider = new KeycloakAuthenticationProvider();
        keycloakAuthProvider.setGrantedAuthoritiesMapper(authorityMapper);

        return keycloakAuthProvider;
    }
}
