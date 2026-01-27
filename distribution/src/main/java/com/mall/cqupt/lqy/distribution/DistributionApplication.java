package com.mall.cqupt.lqy.distribution;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Description: 分发模块｜负责按批次分发用户优惠券，可提供应用弹框推送、站内信或短信通知等
 * @Author: liuqiyang
 */
@SpringBootApplication
@MapperScan("com.mall.cqupt.lqy.distribution.dao.mapper")
@EnableFeignClients("com.mall.cqupt.lqy.distribution.remote")
public class DistributionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributionApplication.class, args);
    }
}
