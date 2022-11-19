package com.rdfsonto.rdfsonto.controller;

import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.controller.project.ProjectController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
public class ImportController
{
    private final ProjectController repository;
}

