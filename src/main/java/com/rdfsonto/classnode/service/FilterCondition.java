package com.rdfsonto.classnode.service;

import lombok.RequiredArgsConstructor;


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
