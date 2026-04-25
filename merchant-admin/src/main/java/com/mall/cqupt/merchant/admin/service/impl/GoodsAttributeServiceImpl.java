package com.mall.cqupt.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsAttributeDO;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsAttributeMapper;
import com.mall.cqupt.merchant.admin.dto.req.GoodsAttributeSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsAttributeRespDTO;
import com.mall.cqupt.merchant.admin.service.GoodsAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsAttributeServiceImpl extends ServiceImpl<GoodsAttributeMapper, GoodsAttributeDO> implements GoodsAttributeService {

    private final GoodsAttributeMapper goodsAttributeMapper;

    @NoDuplicateSubmit(message = "请勿短时间内重复创建属性")
    @Override
    public void createAttribute(GoodsAttributeSaveReqDTO requestParam) {
        checkAttributePermission();

        if (requestParam.getInputType() != null && requestParam.getInputType() == 1
                && (requestParam.getValues() == null || requestParam.getValues().isEmpty())) {
            throw new ClientException("单选类型属性必须提供可选值");
        }

        GoodsAttributeDO attributeDO = BeanUtil.toBean(requestParam, GoodsAttributeDO.class);
        attributeDO.setStatus(0);
        goodsAttributeMapper.insert(attributeDO);
    }

    @NoDuplicateSubmit(message = "请勿短时间内重复修改属性")
    @Override
    public void updateAttribute(String attributeId, GoodsAttributeSaveReqDTO requestParam) {
        checkAttributePermission();

        GoodsAttributeDO existing = goodsAttributeMapper.selectById(attributeId);
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ClientException("属性不存在");
        }

        GoodsAttributeDO updateDO = BeanUtil.toBean(requestParam, GoodsAttributeDO.class);
        updateDO.setId(existing.getId());
        updateDO.setStatus(existing.getStatus());
        goodsAttributeMapper.updateById(updateDO);
    }

    @Override
    public void deleteAttribute(String attributeId) {
        checkAttributePermission();

        GoodsAttributeDO existing = goodsAttributeMapper.selectById(attributeId);
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ClientException("属性不存在");
        }

        GoodsAttributeDO updateDO = GoodsAttributeDO.builder().delFlag(1).build();
        updateDO.setId(existing.getId());
        goodsAttributeMapper.updateById(updateDO);
    }

    @Override
    public List<GoodsAttributeRespDTO> listAllAttributes() {
        LambdaQueryWrapper<GoodsAttributeDO> queryWrapper = Wrappers.lambdaQuery(GoodsAttributeDO.class)
                .eq(GoodsAttributeDO::getDelFlag, 0)
                .eq(GoodsAttributeDO::getStatus, 0)
                .orderByAsc(GoodsAttributeDO::getSortOrder);
        List<GoodsAttributeDO> attributes = goodsAttributeMapper.selectList(queryWrapper);
        return attributes.stream()
                .map(attr -> BeanUtil.toBean(attr, GoodsAttributeRespDTO.class))
                .collect(Collectors.toList());
    }

    private void checkAttributePermission() {
        Integer roleType = UserContext.getRoleType();
        if (UserRoleEnum.PLATFORM.getType().equals(roleType)) {
            return;
        }
        if (UserRoleEnum.MERCHANT.getType().equals(roleType) && UserContext.getShopNumber() != null) {
            return;
        }
        throw new ClientException("当前功能仅平台人员或商家角色可操作");
    }
}
