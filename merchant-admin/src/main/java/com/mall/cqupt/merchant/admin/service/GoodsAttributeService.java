package com.mall.cqupt.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsAttributeDO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsAttributeSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsAttributeRespDTO;

import java.util.List;

public interface GoodsAttributeService extends IService<GoodsAttributeDO> {

    void createAttribute(GoodsAttributeSaveReqDTO requestParam);

    void updateAttribute(String attributeId, GoodsAttributeSaveReqDTO requestParam);

    void deleteAttribute(String attributeId);

    List<GoodsAttributeRespDTO> listAllAttributes();
}
