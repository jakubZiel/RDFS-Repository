package com.rdfsonto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@EnableCaching
@EnableFeignClients
@SpringBootApplication
@EnableNeo4jRepositories
public class RdfsOntoApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(RdfsOntoApplication.class, args);
    }
}
