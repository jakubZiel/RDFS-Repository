package com.rdfsonto.rdfsonto.controller;

import java.util.Collection;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.model.UserNode;
import com.rdfsonto.rdfsonto.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/user")
public class UserController
{
    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public Optional<UserNode> getUserById(@PathVariable final long id)
    {
        return userRepository.findById(id);
    }

    @GetMapping("/by_name/{username}")
    public Optional<UserNode> getUserByName(@PathVariable final String username)
    {
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public Collection<UserNode> getAllUsers()
    {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<UserNode> create(final UserNode user)
    {
        if (userRepository.findByUsername(user.getUsername()).isPresent())
        {
            log.info("User name: {} already exist. Can not be created.", user.getUsername());
            return ResponseEntity.badRequest().build();
        }

        final var created = userRepository.save(user);

        return ResponseEntity.ok(created);
    }

    @PutMapping
    public ResponseEntity<UserNode> update(final UserNode update)
    {
        final var original = userRepository.findById(update.getId());

        if (original.isEmpty())
        {
            log.info("User id: {} does not exist, can not be updated.", update.getId());

            return ResponseEntity.notFound().build();
        }

        final var updated = userRepository.save(update);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserNode> delete(@PathVariable final long id)
    {
        final var user = userRepository.findById(id);

        if (user.isEmpty())
        {
            log.info("User id: {} can not be deleted, because it does not exist", id);
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
