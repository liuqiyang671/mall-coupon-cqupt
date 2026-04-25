package com.mall.cqupt.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsDO;
import org.apache.ibatis.annotations.Param;

public interface GoodsMapper extends BaseMapper<GoodsDO> {

    int decreaseStock(@Param("shopNumber") Long shopNumber,
                      @Param("goodsId") Long goodsId,
                      @Param("quantity") Integer quantity);

    int increaseStock(@Param("shopNumber") Long shopNumber,
                      @Param("goodsId") Long goodsId,
                      @Param("quantity") Integer quantity);

    int increaseSales(@Param("shopNumber") Long shopNumber,
                      @Param("goodsId") Long goodsId,
                      @Param("quantity") Integer quantity);
}
