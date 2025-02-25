package com.rdfsonto.classnode.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(setterPrefix = "with", toBuilder = true)
public record ClassNode(Long id, List<String> classLabels,
                        Map<Long, List<String>> incomingNeighbours,
                        Map<Long, List<String>> outgoingNeighbours,
                        Map<String, Object> properties,
                        String uri)
{
}
