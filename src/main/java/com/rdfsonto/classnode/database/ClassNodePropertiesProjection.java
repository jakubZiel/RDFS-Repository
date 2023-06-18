package com.rdfsonto.classnode.database;

import java.util.List;


public interface ClassNodePropertiesProjection
{
    Long getId();
    String getUri();
    List<String> getClassLabels();
}