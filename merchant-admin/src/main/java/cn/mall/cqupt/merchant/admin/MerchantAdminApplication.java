package cn.mall.cqupt.merchant.admin;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Description: 商家后管服务｜创建优惠券、店家查看以及管理优惠券、创建优惠券发放批次等
 * @Author: liuqiyang
 */
@SpringBootApplication
@MapperScan("cn.mall.cqupt.merchant.admin.dao.mapper")
public class MerchantAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantAdminApplication.class, args);
    }
}
