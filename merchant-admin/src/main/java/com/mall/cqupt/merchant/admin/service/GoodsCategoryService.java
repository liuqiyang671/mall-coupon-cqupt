package com.mall.cqupt.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsCategoryDO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsCategorySaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsCategoryRespDTO;

import java.util.List;

public interface GoodsCategoryService extends IService<GoodsCategoryDO> {

    void createCategory(GoodsCategorySaveReqDTO requestParam);

    void updateCategory(String categoryId, GoodsCategorySaveReqDTO requestParam);

    void deleteCategory(String categoryId);

    GoodsCategoryRespDTO findCategoryById(String categoryId);

    List<GoodsCategoryRespDTO> listCategoryTree();

    void updateCategoryStatus(String categoryId, Integer status);
}
