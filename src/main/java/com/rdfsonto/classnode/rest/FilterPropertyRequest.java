package com.rdfsonto.classnode.rest;

import java.util.List;

import com.rdfsonto.classnode.service.FilterCondition;
import com.rdfsonto.classnode.service.PatternFilter;
import com.rdfsonto.elastic.service.SearchAfterParams;



public record FilterPropertyRequest(List<String> labels, List<FilterCondition> filterConditions, List<PatternFilter> patterns, long projectId, SearchAfterParams searchAfter)
{
}
