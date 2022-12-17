package com.rdfsonto.rdfsonto.service.classnode;

import java.util.List;
import java.util.Optional;


public interface ClassNodeService
{
    List<ClassNode> getClassNodesByIds(List<Long> ids);

    Optional<ClassNode> getClassNodeById(Long id);
}
