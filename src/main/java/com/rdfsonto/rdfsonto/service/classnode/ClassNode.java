package com.rdfsonto.rdfsonto.service.classnode;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(setterPrefix = "with", toBuilder = true)
public record ClassNode(Long id, List<String> classLabels,
                        Map<Long, String> incomingNeighbours,
                        Map<Long, String> outgoingNeighbours,
                        Map<String, String> properties,
                        String uri)
{
}
