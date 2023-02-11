package com.rdfsonto.classnode.rest;

import java.util.List;

import com.rdfsonto.classnode.service.FilterCondition;


public record FilterPropertyRequest(List<String> labels, List<FilterCondition> filterConditions, long projectId)
{
}
