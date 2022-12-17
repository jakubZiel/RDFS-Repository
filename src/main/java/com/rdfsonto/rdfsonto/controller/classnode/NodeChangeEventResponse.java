package com.rdfsonto.rdfsonto.controller.classnode;

import com.rdfsonto.rdfsonto.service.classnode.ClassNode;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record NodeChangeEventResponse(NodeChangeEvent event, boolean failed, ClassNode body)
{
}
