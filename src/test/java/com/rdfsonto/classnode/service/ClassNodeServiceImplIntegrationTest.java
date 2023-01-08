package com.rdfsonto.classnode.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class ClassNodeServiceImplIntegrationTest
{
    @Autowired
    private ClassNodeService classNodeService;

    @Test
    void test()
    {
        final var test = classNodeService.findById(1L);
    }
}
