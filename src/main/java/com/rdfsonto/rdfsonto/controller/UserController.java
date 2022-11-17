package com.rdfsonto.rdfsonto.controller;

import com.rdfsonto.rdfsonto.model.UserNode;
import com.rdfsonto.rdfsonto.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/neo4j/user")
public class UserController
{

    UserRepository repository;

    @Autowired
    public UserController(UserRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public Optional<UserNode> getUserById(@PathVariable long id)
    {
        return repository.findById(id);
    }

    @GetMapping("/by_name/{username}")
    public Optional<UserNode> getUserById(@PathVariable String username)
    {
        return repository.findByUsername(username);
    }

    @GetMapping("all")
    public Collection<UserNode> getAllUsers()
    {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<UserNode> createUser(UserNode user)
    {

        if (repository.findByUsername(user.getUsername()).isPresent())
        {
            return ResponseEntity.badRequest().build();
        }
        repository.save(user);
        return ResponseEntity.ok(user);
    }
}
