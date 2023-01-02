package com.rdfsonto.rdfsonto.classnode.rest;

import com.rdfsonto.rdfsonto.classnode.service.ClassNode;


public record NodeChangeEvent(long nodeId, ClassNode body, ChangeEventType type)
{
    public enum ChangeEventType
    {
        DELETE, UPDATE, CREATE
    }
}