package com.rdfsonto.classnode.rest;

import java.util.List;

import com.rdfsonto.classnode.service.FilterCondition;
import com.rdfsonto.classnode.service.PatternFilter;


public record FilterPropertyRequest(List<String> labels, List<FilterCondition> filterConditions, List<PatternFilter> patterns, long projectId)
{
}
