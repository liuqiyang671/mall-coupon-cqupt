package com.mall.cqupt.engine;


import com.mall.cqupt.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.host=43.139.79.212",// 直接在这里强制指定 IP
        "spring.data.redis.port=6379",
        "spring.data.redis.password=Lqy259931",

})
@TestPropertySource(locations = "classpath:application.yaml")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CouponTemplateServiceRemindTests {
    @Resource
    private CouponTemplateRemindService couponTemplateRemindService;

    @Test
    void testQuery() {
        CouponTemplateRemindQueryReqDTO req = new CouponTemplateRemindQueryReqDTO();
        req.setUserId("1810868149847928832");

        List<CouponTemplateRemindQueryRespDTO> resp = couponTemplateRemindService.listCouponRemind(req);
        for (CouponTemplateRemindQueryRespDTO couponTemplateRemindQueryRespDTO : resp) {
            System.out.println(couponTemplateRemindQueryRespDTO.toString());
        }
    }
}
