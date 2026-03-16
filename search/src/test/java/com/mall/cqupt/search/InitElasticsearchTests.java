package com.mall.cqupt.search;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

@Slf4j
@SpringBootTest
public class InitElasticsearchTests {

    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test

    public void testElasticsearchTemplate() {
        String clusterVersion = elasticsearchTemplate.getClusterVersion();
        log.info(clusterVersion);
    }

}
