package com.rdfsonto.classnode.service;

import com.rdfsonto.classnode.database.RelationshipDirection;

import lombok.Builder;
import lombok.Data;


@Data
@Builder(setterPrefix = "with")
public class PatternFilter
{
    final String relationshipName;
    final RelationshipDirection direction;
}
