package com.rdfsonto.rdfsonto.infrastructure.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;


@Service
@Component
@RequiredArgsConstructor
class AuthKeycloakClient
{
    private static final String CLIENT_ID = "admin-cli";
    private static final String KEYCLOAK_ACCESS_TOKEN_URL = "http://localhost:8888/auth/realms/ontology-editor/protocol/openid-connect/token";
    private static final String KEYCLOAK_CREATE_USER_URL = "http://localhost:8888/auth/admin/realms/ontology-editor/users";
    private static final String CREATE_KEYCLOAK_USER_REQUEST_BODY_TEMPLATE = """
        {
                "enabled": true,
                "username": "%s",
                "firstName": "%s",
                "lastName": "%s",
                "email": "%s",
                "credentials": [
                  {
                    "type": "password",
                    "value": "%s",
                    "temporary": false
                  }],
                "realmRoles": [	"user" ]
        }
        """;
    private final RestTemplate restTemplate;
    private final JSONParser jsonParser;
    @Value("${custom.keycloak.client.secret}")
    private String CLIENT_SECRET;

    KeycloakUser createKeycloakUser(final KeycloakUser keycloakUser) throws ParseException
    {

        final var httpEntityTokenRequest = prepareHttpEntityTokenRequest();

        final var tokenResponse = restTemplate.exchange(
            KEYCLOAK_ACCESS_TOKEN_URL,
            HttpMethod.POST,
            httpEntityTokenRequest,
            String.class);

        final var accessToken = extractAccessToken(tokenResponse);

        final var httpEntityCreateKeycloakUserRequest = prepareHttpEntityCreateUserRequest(accessToken, keycloakUser);

        final var createUserResponse = restTemplate.exchange(
            KEYCLOAK_CREATE_USER_URL,
            HttpMethod.POST,
            httpEntityCreateKeycloakUserRequest,
            String.class);

        final var keycloakUserId = extractKeycloakUserId(createUserResponse);

        return keycloakUser.toBuilder()
            .withKeycloakId(keycloakUserId)
            .build();
    }

    private HttpEntity<?> prepareHttpEntityTokenRequest()
    {
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final var form = new LinkedMultiValueMap<>();
        form.add("client_id", CLIENT_ID);
        form.add("grant_type", "client_credentials");
        form.add("client_secret", CLIENT_SECRET);

        return new HttpEntity<>(form, headers);
    }

    private String extractAccessToken(final ResponseEntity<String> tokenResponse) throws ParseException
    {
        final var body = tokenResponse.getBody();

        final var json = (JSONObject) jsonParser.parse(body);

        return (String) json.get("access_token");
    }

    private HttpEntity<?> prepareHttpEntityCreateUserRequest(final String accessToken, final KeycloakUser keycloakUser)
    {
        final var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer %s".formatted(accessToken));
        headers.setContentType(MediaType.APPLICATION_JSON);

        final var body = CREATE_KEYCLOAK_USER_REQUEST_BODY_TEMPLATE.formatted(
            keycloakUser.username(),
            keycloakUser.firstName(),
            keycloakUser.lastName(),
            keycloakUser.email(),
            keycloakUser.password()
        );

        return new HttpEntity<>(body, headers);
    }

    private String extractKeycloakUserId(final ResponseEntity<?> createKeycloakUserResponse)
    {
        final var headers = createKeycloakUserResponse.getHeaders();

        final var location = headers.getLocation();

        if (location == null)
        {
            return null;
        }

        final var indexOfId = location.getPath().lastIndexOf('/');

        return location.getPath().substring(indexOfId + 1);
    }
}


