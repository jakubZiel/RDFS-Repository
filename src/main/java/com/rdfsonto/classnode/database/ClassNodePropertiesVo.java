package com.rdfsonto.classnode.database;

import java.util.List;
import java.util.Map;

import lombok.Builder;


@Builder(setterPrefix = "with")
record ClassNodePropertiesVo(Long nodeId, Map<String, Object> properties, List<String> labels)
{
}
