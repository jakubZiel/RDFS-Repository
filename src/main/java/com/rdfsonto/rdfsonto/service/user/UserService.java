package com.rdfsonto.rdfsonto.service.user;

import java.util.List;
import java.util.Optional;

import com.rdfsonto.rdfsonto.repository.user.UserNode;


public interface UserService
{
    Optional<UserNode> findById(long userId);

    Optional<UserNode> findByUsername(String username);

    List<UserNode> findAll();

    UserNode save(UserNode user);

    void delete(UserNode user);
}
