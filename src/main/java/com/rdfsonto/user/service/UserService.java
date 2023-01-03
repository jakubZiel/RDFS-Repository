package com.rdfsonto.user.service;

import java.util.List;
import java.util.Optional;

import com.rdfsonto.user.database.UserNode;


public interface UserService
{
    Optional<UserNode> findById(long userId);

    Optional<UserNode> findByUsername(String username);

    List<UserNode> findAll();

    UserNode save(UserNode user);

    void delete(UserNode user);
}
