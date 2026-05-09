package com.mall.cqupt.merchant.admin.user;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.MD5;
import com.github.javafaker.Faker;
import com.mall.cqupt.merchant.admin.dao.entity.UserDO;
import com.mall.cqupt.merchant.admin.dao.mapper.UserMapper;
import jodd.util.ThreadUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bulk user data generator; intentionally runs only as an integration test.
 */
@Tag("integration")
@SpringBootTest
public class MockUserDataIT {

    @Autowired
    private UserMapper userMapper;

    private final Faker faker = new Faker(Locale.CHINA);
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            10,
            10,
            9999,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    private final int maxNum = 500000;

    public void beforeDataBuild() {
        for (int i = 0; i < 20; i++) {
            snowflakes.add(new Snowflake(i));
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void mockUserTest() {
        beforeDataBuild();
        AtomicInteger count = new AtomicInteger(0);
        while (count.get() < maxNum) {
            executorService.execute(() -> {
                ThreadUtil.sleep(RandomUtil.randomInt(1000));
                UserDO userDO = UserDO.builder()
                        .id(snowflakes.get(RandomUtil.randomInt(20)).nextId())
                        .shopNumber("453055583")
                        .username(faker.funnyName().name())
                        .phone(faker.phoneNumber().cellPhone())
                        .password(MD5.create().digestHex(faker.number().digits(10)))
                        .mail(faker.number().digits(10) + "@163.com")
                        .build();
                userMapper.insert(userDO);
                count.incrementAndGet();
            });
        }
    }
}
