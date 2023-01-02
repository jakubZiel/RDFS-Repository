package com.rdfsonto.rdfsonto.classnode.rest;

import com.rdfsonto.rdfsonto.classnode.service.ClassNode;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record NodeChangeEventResponse(NodeChangeEvent event, boolean failed, ClassNode body)
{
}
