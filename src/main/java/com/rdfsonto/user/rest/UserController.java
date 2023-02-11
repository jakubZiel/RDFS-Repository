package com.rdfsonto.user.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.user.database.UserNode;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/user")
public class UserController
{
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserNode> getUserById(@PathVariable final long id)
    {
        final var user = userService.findById(id);

        if (user.isEmpty())
        {
            log.info("User id: {} does not exist.", id);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.of(user);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserNode> getUserByUserName(@PathVariable final String username)
    {
        final var user = userService.findByUsername(username);

        if (user.isEmpty())
        {
            log.info("User with username: {} does not exist.", username);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.of(user);
    }

    @GetMapping
    public ResponseEntity<List<UserNode>> getAllUsers()
    {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping
    public ResponseEntity<UserNode> create(final UserNode user)
    {
        if (userService.findByUsername(user.getUsername()).isPresent())
        {
            log.info("User name: {} already exist. Can not be created", user.getUsername());
            return ResponseEntity.badRequest().build();
        }

        final var created = userService.save(user);

        return ResponseEntity.ok(created);
    }

    @PutMapping
    public ResponseEntity<UserNode> update(final UserNode update)
    {
        final var original = userService.findById(update.getId());

        if (original.isEmpty())
        {
            log.info("User id: {} does not exist, can not be updated", update.getId());

            return ResponseEntity.notFound().build();
        }

        final var updated = userService.save(update);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable final long id)
    {
        final var user = userService.findById(id);

        if (user.isEmpty())
        {
            log.info("User id: {} can not be deleted, because it does not exist", id);
            return ResponseEntity.notFound().build();
        }

        userService.delete(user.get());

        return ResponseEntity.noContent().build();
    }
}
