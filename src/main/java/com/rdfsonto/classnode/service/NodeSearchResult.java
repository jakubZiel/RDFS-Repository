package com.rdfsonto.classnode.service;

import java.util.List;

import com.rdfsonto.elastic.service.SearchAfterParams;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record NodeSearchResult(List<ClassNode> nodes, SearchAfterParams searchAfter)
{
}
