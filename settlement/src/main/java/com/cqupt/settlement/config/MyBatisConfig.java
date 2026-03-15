package com.cqupt.settlement.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis 配置类
 */
@Configuration
@MapperScan("com.cqupt.settlement.dao.mapper")
public class MyBatisConfig {

    /**
     * 创建并配置 SqlSessionFactory 实例
     *
     * @param dataSource 数据源
     * @return 配置好的 SqlSessionFactory 实例
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        return sessionFactory.getObject();
    }
}
