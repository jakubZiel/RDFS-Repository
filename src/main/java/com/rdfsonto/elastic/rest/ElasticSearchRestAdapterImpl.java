package com.rdfsonto.elastic.rest;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.classnode.service.ClassNode;
import com.rdfsonto.classnode.service.ClassNodeService;
import com.rdfsonto.elastic.service.ElasticSearchClassNodeBulkService;
import com.rdfsonto.elastic.service.ElasticSearchClassNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/elastic")
public class ElasticSearchRestAdapterImpl
{
    private final ElasticSearchClassNodeService elasticSearchClassNodeService;
    private final ElasticSearchClassNodeBulkService elasticSearchClassNodeBulkService;
    private final ClassNodeService classNodeService;

    @GetMapping
    public void testElastic()
    {
        elasticSearchClassNodeBulkService.createIndex(34L, 35L);
    }

    @GetMapping("search")
    public void testElasticSearch()
    {

        final var pageSize = 70_000;
        var start = System.currentTimeMillis();
        var res = classNodeService.findByProject(35, Pageable.ofSize(pageSize).withPage(0));
        var end = System.currentTimeMillis();
        log.warn("time elapsed");
        log.warn(String.valueOf(end - start));


        var resIds = res.stream()
            .map(ClassNode::id)
            .toList();

        start = System.currentTimeMillis();
        res = classNodeService.findByIdsLight(35, resIds);
        end = System.currentTimeMillis();
        log.warn(String.valueOf(end - start));
        log.warn("time elapsed - by ids");
        log.warn(String.valueOf(end - start));



        start = System.currentTimeMillis();
        res = classNodeService.findByProject(35, Pageable.ofSize(pageSize).withPage(1));
        end = System.currentTimeMillis();
        log.warn("time elapsed");
        log.warn(String.valueOf(end - start));

        start = System.currentTimeMillis();
        res = classNodeService.findByProject(35, Pageable.ofSize(pageSize).withPage(2));
        end = System.currentTimeMillis();
        log.warn("time elapsed");
        log.warn(String.valueOf(end - start));

    }
}
