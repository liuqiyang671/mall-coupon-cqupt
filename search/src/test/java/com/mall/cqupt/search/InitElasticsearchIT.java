package com.mall.cqupt.search;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

/**
 * Elasticsearch integration smoke test; requires a reachable Elasticsearch cluster.
 */
@Slf4j
@Tag("integration")
@SpringBootTest
public class InitElasticsearchIT {

    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void testElasticsearchTemplate() {
        String clusterVersion = elasticsearchTemplate.getClusterVersion();
        log.info(clusterVersion);
    }
}
