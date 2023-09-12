package com.rdfsonto.infrastructure.config.security;

import java.util.List;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


@KeycloakConfiguration
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter
{
    @Autowired
    void configureGlobal(final AuthenticationManagerBuilder authenticationManagerBuilder)
    {
        final var authProvider = new KeycloakAuthenticationProvider();
        authProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());

        authenticationManagerBuilder.authenticationProvider(authProvider);
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy()
    {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    public FilterRegistrationBean corsFilter()
    {
        final var source = new UrlBasedCorsConfigurationSource();
        final var config = new CorsConfiguration().applyPermitDefaultValues();

        config.addAllowedMethod(HttpMethod.DELETE);
        config.addAllowedMethod(HttpMethod.PUT);

        config.setExposedHeaders(List.of("Content-Disposition"));

        source.registerCorsConfiguration("/**", config);
        final var bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception
    {
        super.configure(http);

        http.addFilter(corsFilter().getFilter());
        http.csrf().disable();

        http.authorizeRequests()
            .anyRequest()
            .permitAll();
    }
}
