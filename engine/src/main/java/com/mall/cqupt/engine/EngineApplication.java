package com.mall.cqupt.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Description: 引擎服务｜负责优惠券单个查看、列表查看、锁定以及核销等功能
 * @Author: liuqiyang
 */
@SpringBootApplication
@MapperScan("com.mall.cqupt.engine.dao.mapper")
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }
}
