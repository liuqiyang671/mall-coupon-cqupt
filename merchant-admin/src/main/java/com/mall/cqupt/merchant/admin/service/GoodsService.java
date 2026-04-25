package com.mall.cqupt.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsDO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsStockReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserGoodsPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsPageQueryRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsQueryRespDTO;

import java.util.List;

public interface GoodsService extends IService<GoodsDO> {

    void createGoods(GoodsSaveReqDTO requestParam);

    void updateGoods(String goodsId, GoodsSaveReqDTO requestParam);

    IPage<GoodsPageQueryRespDTO> pageQueryGoods(GoodsPageQueryReqDTO requestParam);

    GoodsQueryRespDTO findGoodsById(String goodsId);

    IPage<GoodsPageQueryRespDTO> pageQueryUserGoods(UserGoodsPageQueryReqDTO requestParam);

    GoodsQueryRespDTO findUserGoodsById(String goodsId, Long shopNumber);

    void updateGoodsStatus(String goodsId, Integer status);

    void adjustStock(GoodsStockReqDTO requestParam);

    void deleteGoods(String goodsId);

    List<GoodsQueryRespDTO> listGoodsByIds(List<String> goodsIds);
}
