package com.mall.cqupt.engine;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring context integration smoke test, kept out of the default unit-test run.
 */
@Tag("integration")
@SpringBootTest
class EngineApplicationIT {

    @Test
    void contextLoads() {
    }
}
