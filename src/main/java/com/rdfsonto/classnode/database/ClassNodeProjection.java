package com.rdfsonto.classnode.database;

import java.util.List;
import java.util.Map;


public interface ClassNodeProjection extends ClassNodePropertiesProjection
{
    Map<String, List<ClassNodeProjection>> getNeighbours();
}