package com.mall.cqupt.engine;

import com.mall.cqupt.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * Redis-backed integration test, isolated from default mvn test by the *IT naming convention.
 */
@Tag("integration")
@SpringBootTest
@TestPropertySource(
        locations = "classpath:application.yaml",
        properties = {
                "spring.data.redis.host=${TEST_REDIS_HOST:127.0.0.1}",
                "spring.data.redis.port=${TEST_REDIS_PORT:6379}",
                "spring.data.redis.password=${TEST_REDIS_PASSWORD:}"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CouponTemplateServiceRemindIT {

    @Resource
    private CouponTemplateRemindService couponTemplateRemindService;

    @Test
    void testQuery() {
        CouponTemplateRemindQueryReqDTO req = new CouponTemplateRemindQueryReqDTO();
        req.setUserId("1810868149847928832");

        List<CouponTemplateRemindQueryRespDTO> resp = couponTemplateRemindService.listCouponRemind(req);
        for (CouponTemplateRemindQueryRespDTO couponTemplateRemindQueryRespDTO : resp) {
            System.out.println(couponTemplateRemindQueryRespDTO);
        }
    }
}
