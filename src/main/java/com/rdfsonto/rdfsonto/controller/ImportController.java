package com.rdfsonto.rdfsonto.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
public class ImportController
{
    ProjectController repository;

    @Autowired
    public ImportController(ProjectController repository)
    {
        this.repository = repository;
    }
}
