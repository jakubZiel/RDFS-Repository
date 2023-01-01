package com.rdfsonto.rdfsonto.service.classnode;

import java.util.List;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record ProjectNodeMetadata(List<String> propertyKeys, List<String> nodeLabels, List<String> relationshipTypes)
{
}
