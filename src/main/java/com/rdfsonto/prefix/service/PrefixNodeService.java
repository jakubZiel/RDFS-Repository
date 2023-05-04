package com.rdfsonto.prefix.service;

import java.util.Map;
import java.util.Optional;


public interface PrefixNodeService
{
    Optional<Map<String, String>> findAll(long projectId);

    Map<String, String> save(long projectId, Map<String, String> prefixes);

    void delete(long projectId);
}
