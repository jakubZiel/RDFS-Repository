package com.rdfsonto.rdfsonto.service.classnode;

import java.util.List;
import java.util.Optional;


public interface ClassNodeService
{
    List<ClassNode> findByIds(List<Long> ids);

    Optional<ClassNode> findById(Long id);

    Optional<ClassNode> save(ClassNode node);

    Optional<ClassNode> update(ClassNode node);

    boolean deleteById(long id);
}
