package com.rdfsonto.prefix.service;

import java.util.Map;
import java.util.Optional;


public interface PrefixNodeService
{
    Optional<PrefixMapping> findAll(long projectId);

    PrefixMapping save(long projectId, Map<String, String> prefixes);

    void delete(long projectId);
}
