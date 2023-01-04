package com.rdfsonto.classnode.database;

import java.util.Map;

import lombok.Builder;


@Builder(setterPrefix = "with")
record ClassNodePropertiesVo(Long nodeId, Map<String, String> properties)
{
}
