package com.rdfsonto.classnode.rest;

import com.rdfsonto.classnode.service.ClassNode;


public record NodeChangeEvent(long nodeId, ClassNode body, ChangeEventType type)
{
    public enum ChangeEventType
    {
        DELETE, UPDATE, CREATE
    }
}