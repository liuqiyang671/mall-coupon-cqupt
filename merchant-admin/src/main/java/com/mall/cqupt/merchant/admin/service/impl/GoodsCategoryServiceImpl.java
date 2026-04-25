package com.mall.cqupt.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.enums.GoodsCategoryStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsCategoryDO;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsDO;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsCategoryMapper;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsMapper;
import com.mall.cqupt.merchant.admin.dto.req.GoodsCategorySaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsCategoryRespDTO;
import com.mall.cqupt.merchant.admin.service.GoodsCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsCategoryServiceImpl extends ServiceImpl<GoodsCategoryMapper, GoodsCategoryDO> implements GoodsCategoryService {

    private final GoodsCategoryMapper goodsCategoryMapper;
    private final GoodsMapper goodsMapper;

    @NoDuplicateSubmit(message = "请勿短时间内重复创建分类")
    @Override
    public void createCategory(GoodsCategorySaveReqDTO requestParam) {
        checkCategoryPermission();

        if (requestParam.getParentId() == null) {
            requestParam.setParentId(0L);
        }

        int level = 1;
        if (requestParam.getParentId() > 0) {
            GoodsCategoryDO parent = goodsCategoryMapper.selectById(requestParam.getParentId());
            if (parent == null || parent.getDelFlag() == 1) {
                throw new ClientException("父分类不存在");
            }
            if (parent.getLevel() >= 3) {
                throw new ClientException("最多支持三级分类");
            }
            level = parent.getLevel() + 1;
        }

        LambdaQueryWrapper<GoodsCategoryDO> nameCheck = Wrappers.lambdaQuery(GoodsCategoryDO.class)
                .eq(GoodsCategoryDO::getName, requestParam.getName())
                .eq(GoodsCategoryDO::getParentId, requestParam.getParentId())
                .eq(GoodsCategoryDO::getDelFlag, 0);
        if (goodsCategoryMapper.selectCount(nameCheck) > 0) {
            throw new ClientException("同级下已存在同名分类");
        }

        GoodsCategoryDO categoryDO = BeanUtil.toBean(requestParam, GoodsCategoryDO.class);
        categoryDO.setLevel(level);
        categoryDO.setStatus(GoodsCategoryStatusEnum.ENABLED.getStatus());
        goodsCategoryMapper.insert(categoryDO);
    }

    @NoDuplicateSubmit(message = "请勿短时间内重复修改分类")
    @Override
    public void updateCategory(String categoryId, GoodsCategorySaveReqDTO requestParam) {
        checkCategoryPermission();

        GoodsCategoryDO existing = goodsCategoryMapper.selectById(categoryId);
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ClientException("分类不存在");
        }

        if (requestParam.getName() != null && !requestParam.getName().equals(existing.getName())) {
            LambdaQueryWrapper<GoodsCategoryDO> nameCheck = Wrappers.lambdaQuery(GoodsCategoryDO.class)
                    .eq(GoodsCategoryDO::getName, requestParam.getName())
                    .eq(GoodsCategoryDO::getParentId, existing.getParentId())
                    .eq(GoodsCategoryDO::getDelFlag, 0)
                    .ne(GoodsCategoryDO::getId, categoryId);
            if (goodsCategoryMapper.selectCount(nameCheck) > 0) {
                throw new ClientException("同级下已存在同名分类");
            }
        }

        GoodsCategoryDO updateDO = BeanUtil.toBean(requestParam, GoodsCategoryDO.class);
        updateDO.setId(existing.getId());
        updateDO.setLevel(existing.getLevel());
        goodsCategoryMapper.updateById(updateDO);
    }

    @Override
    public void deleteCategory(String categoryId) {
        checkCategoryPermission();

        GoodsCategoryDO existing = goodsCategoryMapper.selectById(categoryId);
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ClientException("分类不存在");
        }

        LambdaQueryWrapper<GoodsCategoryDO> childCheck = Wrappers.lambdaQuery(GoodsCategoryDO.class)
                .eq(GoodsCategoryDO::getParentId, categoryId)
                .eq(GoodsCategoryDO::getDelFlag, 0);
        if (goodsCategoryMapper.selectCount(childCheck) > 0) {
            throw new ClientException("存在子分类，不能删除");
        }

        Long shopNumber = resolveShopNumber();
        LambdaQueryWrapper<GoodsDO> goodsCheck = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getCategoryId, categoryId)
                .eq(GoodsDO::getDelFlag, 0);
        if (UserRoleEnum.MERCHANT.getType().equals(UserContext.getRoleType())) {
            goodsCheck.eq(GoodsDO::getShopNumber, shopNumber);
        }
        if (goodsMapper.selectCount(goodsCheck) > 0) {
            throw new ClientException("分类下存在商品，不能删除");
        }

        GoodsCategoryDO updateDO = GoodsCategoryDO.builder().delFlag(1).build();
        updateDO.setId(existing.getId());
        goodsCategoryMapper.updateById(updateDO);
    }

    @Override
    public GoodsCategoryRespDTO findCategoryById(String categoryId) {
        GoodsCategoryDO categoryDO = goodsCategoryMapper.selectById(categoryId);
        if (categoryDO == null || categoryDO.getDelFlag() == 1) {
            throw new ClientException("分类不存在");
        }
        return BeanUtil.toBean(categoryDO, GoodsCategoryRespDTO.class);
    }

    @Override
    public List<GoodsCategoryRespDTO> listCategoryTree() {
        LambdaQueryWrapper<GoodsCategoryDO> queryWrapper = Wrappers.lambdaQuery(GoodsCategoryDO.class)
                .eq(GoodsCategoryDO::getDelFlag, 0)
                .eq(GoodsCategoryDO::getStatus, GoodsCategoryStatusEnum.ENABLED.getStatus())
                .orderByAsc(GoodsCategoryDO::getSortOrder);
        List<GoodsCategoryDO> allCategories = goodsCategoryMapper.selectList(queryWrapper);

        List<GoodsCategoryRespDTO> dtoList = allCategories.stream()
                .map(cat -> BeanUtil.toBean(cat, GoodsCategoryRespDTO.class))
                .collect(Collectors.toList());

        return buildTree(dtoList, 0L);
    }

    @Override
    public void updateCategoryStatus(String categoryId, Integer status) {
        checkCategoryPermission();
        if (status != GoodsCategoryStatusEnum.ENABLED.getStatus()
                && status != GoodsCategoryStatusEnum.DISABLED.getStatus()) {
            throw new ClientException("分类状态值不合法");
        }

        GoodsCategoryDO existing = goodsCategoryMapper.selectById(categoryId);
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ClientException("分类不存在");
        }

        if (status == GoodsCategoryStatusEnum.DISABLED.getStatus()) {
            LambdaQueryWrapper<GoodsCategoryDO> childCheck = Wrappers.lambdaQuery(GoodsCategoryDO.class)
                    .eq(GoodsCategoryDO::getParentId, categoryId)
                    .eq(GoodsCategoryDO::getDelFlag, 0)
                    .eq(GoodsCategoryDO::getStatus, GoodsCategoryStatusEnum.ENABLED.getStatus());
            if (goodsCategoryMapper.selectCount(childCheck) > 0) {
                throw new ClientException("存在启用的子分类，不能禁用");
            }
        }

        GoodsCategoryDO updateDO = GoodsCategoryDO.builder().status(status).build();
        updateDO.setId(existing.getId());
        goodsCategoryMapper.updateById(updateDO);
    }

    private List<GoodsCategoryRespDTO> buildTree(List<GoodsCategoryRespDTO> allCategories, Long parentId) {
        Map<Long, List<GoodsCategoryRespDTO>> groupedByParent = allCategories.stream()
                .collect(Collectors.groupingBy(GoodsCategoryRespDTO::getParentId));

        List<GoodsCategoryRespDTO> roots = groupedByParent.getOrDefault(parentId, new ArrayList<>());
        for (GoodsCategoryRespDTO root : roots) {
            root.setChildren(buildTreeRecursive(groupedByParent, root.getId()));
        }
        return roots;
    }

    private List<GoodsCategoryRespDTO> buildTreeRecursive(Map<Long, List<GoodsCategoryRespDTO>> groupedByParent, Long parentId) {
        List<GoodsCategoryRespDTO> children = groupedByParent.getOrDefault(parentId, new ArrayList<>());
        for (GoodsCategoryRespDTO child : children) {
            child.setChildren(buildTreeRecursive(groupedByParent, child.getId()));
        }
        return children;
    }

    private void checkCategoryPermission() {
        Integer roleType = UserContext.getRoleType();
        if (UserRoleEnum.PLATFORM.getType().equals(roleType)) {
            return;
        }
        if (UserRoleEnum.MERCHANT.getType().equals(roleType) && UserContext.getShopNumber() != null) {
            return;
        }
        throw new ClientException("当前功能仅平台人员或商家角色可操作");
    }

    private Long resolveShopNumber() {
        if (UserRoleEnum.PLATFORM.getType().equals(UserContext.getRoleType())) {
            return 0L;
        }
        return UserContext.getShopNumber();
    }
}
