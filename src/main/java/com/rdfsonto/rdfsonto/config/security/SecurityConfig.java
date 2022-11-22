package com.rdfsonto.rdfsonto.config.security;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;


@KeycloakConfiguration
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter
{
    @Autowired
    void configureGlobal(final AuthenticationManagerBuilder authenticationManagerBuilder, final KeycloakAuthenticationProvider keycloakAuthProvider)
    {
        authenticationManagerBuilder.authenticationProvider(keycloakAuthProvider);
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy()
    {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception
    {
        super.configure(http);

        http.cors().and().csrf().disable();

        http.authorizeRequests()
            .antMatchers("/neo4j/*")
            .hasRole("USER")
            .anyRequest()
            .permitAll();
    }
}
