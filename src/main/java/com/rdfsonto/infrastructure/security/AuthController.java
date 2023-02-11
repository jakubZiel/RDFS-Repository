package com.rdfsonto.infrastructure.security;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.infrastructure.security.service.AuthService;
import com.rdfsonto.infrastructure.security.service.KeycloakUser;
import com.rdfsonto.user.database.UserNode;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController
{
    private final AuthService authService;
    private final UserService userService;
    private final EmailValidator emailValidator;

    @GetMapping("/logout")
    public void logout(final HttpServletRequest request) throws ServletException
    {
        request.logout();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerNewUser(@RequestBody final KeycloakUser keycloakUserRequest)
    {
        if (!validate(keycloakUserRequest))
        {
            log.warn("Invalid create keycloak user request : {}", keycloakUserRequest);
            return ResponseEntity.badRequest().body("invalid_body");
        }

        if (userService.findByUsername(keycloakUserRequest.username()).isPresent())
        {
            log.warn("User with username: {} already exists", keycloakUserRequest.username());

            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        final var savedKeycloakUser = authService.save(keycloakUserRequest);
        final var savedUser = userService.save(mapToNode(savedKeycloakUser));

        return ResponseEntity.ok(savedUser);
    }

    private boolean validate(final KeycloakUser keycloakUserRequest)
    {
        return emailValidator.isValid(keycloakUserRequest.email()) &&
            keycloakUserRequest.username() != null && !keycloakUserRequest.username().isBlank() &&
            keycloakUserRequest.password() != null && !keycloakUserRequest.password().isBlank() &&
            keycloakUserRequest.lastName() != null && !keycloakUserRequest.lastName().isBlank() &&
            keycloakUserRequest.firstName() != null && !keycloakUserRequest.firstName().isBlank();
    }

    private UserNode mapToNode(final KeycloakUser keycloakUser)
    {
        return new UserNode(null, keycloakUser.keycloakId(), keycloakUser.username());
    }
}
