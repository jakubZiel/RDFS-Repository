package com.rdfsonto.rdfsonto.service.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rdfsonto.rdfsonto.repository.project.ProjectRepository;
import com.rdfsonto.rdfsonto.repository.user.UserNode;
import com.rdfsonto.rdfsonto.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService
{
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Override
    public Optional<UserNode> findById(final long userId)
    {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<UserNode> findByUsername(final String username)
    {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<UserNode> findAll()
    {
        return userRepository.findAll();
    }

    @Override
    public UserNode save(final UserNode user)
    {
        return userRepository.save(user);
    }

    @Override
    public void delete(final UserNode user)
    {
        if (userRepository.existsById(user.getId()))
        {
            log.warn("Attempted to deleted non-existing user id: {}", user.getId());
            return;
        }

        userRepository.delete(user);
        projectRepository.deleteAll(user.getProjectSet());
    }
}
