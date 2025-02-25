package com.rdfsonto.infrastructure.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.rdfsonto.exportonto.rest.GraphSerializeRequest;
import com.rdfsonto.infrastructure.config.client.Neo4jRdfFeignClientConfig;


@FeignClient(name = "neo4j-rdf", url = "${spring.neo4j.url}", configuration = Neo4jRdfFeignClientConfig.class)
public interface Neo4jRdfClient
{
    @RequestMapping(method = RequestMethod.POST, value = "/rdf/neo4j/cypher")
    String serializeGraphToRdf(@RequestBody GraphSerializeRequest request);
}