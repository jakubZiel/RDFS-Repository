package com.rdfsonto.classnode.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder(setterPrefix = "with", toBuilder = true)
public record FilterCondition(String value, String property, Operator operator)
{
    @RequiredArgsConstructor
    public enum Operator
    {
        CONTAINS("CONTAINS"),
        STARTS_WITH("STARS WITH"),
        ENDS_WITH("ENDS WITH"),
        EQUALS("EQUALS");

        public final String value;

        @Override
        public String toString()
        {
            return value;
        }
    }
}
