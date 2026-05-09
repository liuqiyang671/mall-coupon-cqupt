package com.mall.cqupt.merchant.admin.template;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.RandomUtil;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import com.mall.cqupt.merchant.admin.dao.mapper.CouponTemplateMapper;
import jodd.util.ThreadUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bulk coupon-template data generator; intentionally runs only as an integration test.
 */
@Tag("integration")
@SpringBootTest
public class MockCouponTemplateDataIT {

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    private final CouponTemplateIT couponTemplateTest = new CouponTemplateIT();
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            10,
            10,
            9999,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    private final int maxNum = 5000;

    public void beforeDataBuild() {
        for (int i = 0; i < 20; i++) {
            snowflakes.add(new Snowflake(i));
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    public void mockCouponTemplateTest() {
        beforeDataBuild();
        AtomicInteger count = new AtomicInteger(0);
        while (count.get() < maxNum) {
            executorService.execute(() -> {
                ThreadUtil.sleep(RandomUtil.randomInt(10));
                CouponTemplateDO couponTemplateDO = couponTemplateTest.buildCouponTemplateDO();
                couponTemplateDO.setShopNumber(snowflakes.get(RandomUtil.randomInt(20)).nextId());
                couponTemplateMapper.insert(couponTemplateDO);
                count.incrementAndGet();
            });
        }
    }
}
