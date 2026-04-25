package com.mall.cqupt.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.enums.GoodsStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.*;
import com.mall.cqupt.merchant.admin.dao.mapper.*;
import com.mall.cqupt.merchant.admin.dto.req.GoodsPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsStockReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserGoodsPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsPageQueryRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsQueryRespDTO;
import com.mall.cqupt.merchant.admin.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, GoodsDO> implements GoodsService {

    private final GoodsMapper goodsMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final GoodsImageMapper goodsImageMapper;
    private final GoodsAttributeMapper goodsAttributeMapper;
    private final GoodsAttributeValueMapper goodsAttributeValueMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String GOODS_CACHE_KEY = "one-coupon_merchant-admin:goods:%s";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @NoDuplicateSubmit(message = "请勿短时间内重复创建商品")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createGoods(GoodsSaveReqDTO requestParam) {
        checkGoodsPermission();
        validateGoodsParam(requestParam);

        Long shopNumber = resolveShopNumber();
        GoodsDO goodsDO = BeanUtil.toBean(requestParam, GoodsDO.class);
        goodsDO.setShopNumber(shopNumber);
        goodsDO.setStatus(GoodsStatusEnum.OFF_SHELF.getStatus());
        goodsDO.setSales(0);
        goodsMapper.insert(goodsDO);

        if (CollUtil.isNotEmpty(requestParam.getImageUrls())) {
            List<GoodsImageDO> images = new ArrayList<>();
            for (int i = 0; i < requestParam.getImageUrls().size(); i++) {
                GoodsImageDO imageDO = GoodsImageDO.builder()
                        .shopNumber(shopNumber)
                        .goodsId(goodsDO.getId())
                        .imageUrl(requestParam.getImageUrls().get(i))
                        .sortOrder(i)
                        .build();
                images.add(imageDO);
            }
            images.forEach(goodsImageMapper::insert);
        }

        if (CollUtil.isNotEmpty(requestParam.getAttributeValues())) {
            List<GoodsAttributeValueDO> attrValues = requestParam.getAttributeValues().stream()
                    .map(attr -> GoodsAttributeValueDO.builder()
                            .shopNumber(shopNumber)
                            .goodsId(goodsDO.getId())
                            .attributeId(attr.getAttributeId())
                            .attributeValue(attr.getAttributeValue())
                            .build())
                    .collect(Collectors.toList());
            attrValues.forEach(goodsAttributeValueMapper::insert);
        }

        cacheGoodsDetail(goodsDO);
    }

    @NoDuplicateSubmit(message = "请勿短时间内重复修改商品")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoods(String goodsId, GoodsSaveReqDTO requestParam) {
        checkGoodsPermission();
        validateGoodsParam(requestParam);

        Long shopNumber = resolveShopNumber();
        GoodsDO existingGoods = getGoodsWithOwnerCheck(shopNumber, goodsId);

        GoodsDO updateDO = BeanUtil.toBean(requestParam, GoodsDO.class);
        updateDO.setId(existingGoods.getId());
        updateDO.setShopNumber(shopNumber);
        LambdaQueryWrapper<GoodsDO> updateWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getId, goodsId)
                .eq(GoodsDO::getShopNumber, shopNumber);
        goodsMapper.update(updateDO, updateWrapper);

        LambdaQueryWrapper<GoodsImageDO> imageDeleteWrapper = Wrappers.lambdaQuery(GoodsImageDO.class)
                .eq(GoodsImageDO::getGoodsId, goodsId)
                .eq(GoodsImageDO::getShopNumber, shopNumber);
        goodsImageMapper.delete(imageDeleteWrapper);

        if (CollUtil.isNotEmpty(requestParam.getImageUrls())) {
            for (int i = 0; i < requestParam.getImageUrls().size(); i++) {
                GoodsImageDO imageDO = GoodsImageDO.builder()
                        .shopNumber(shopNumber)
                        .goodsId(Long.parseLong(goodsId))
                        .imageUrl(requestParam.getImageUrls().get(i))
                        .sortOrder(i)
                        .build();
                goodsImageMapper.insert(imageDO);
            }
        }

        LambdaQueryWrapper<GoodsAttributeValueDO> attrDeleteWrapper = Wrappers.lambdaQuery(GoodsAttributeValueDO.class)
                .eq(GoodsAttributeValueDO::getGoodsId, goodsId)
                .eq(GoodsAttributeValueDO::getShopNumber, shopNumber);
        goodsAttributeValueMapper.delete(attrDeleteWrapper);

        if (CollUtil.isNotEmpty(requestParam.getAttributeValues())) {
            List<GoodsAttributeValueDO> attrValues = requestParam.getAttributeValues().stream()
                    .map(attr -> GoodsAttributeValueDO.builder()
                            .shopNumber(shopNumber)
                            .goodsId(Long.parseLong(goodsId))
                            .attributeId(attr.getAttributeId())
                            .attributeValue(attr.getAttributeValue())
                            .build())
                    .collect(Collectors.toList());
            attrValues.forEach(goodsAttributeValueMapper::insert);
        }

        deleteGoodsCache(goodsId);
        cacheGoodsDetail(goodsMapper.selectById(goodsId));
    }

    @Override
    public IPage<GoodsPageQueryRespDTO> pageQueryGoods(GoodsPageQueryReqDTO requestParam) {
        checkGoodsPermission();

        Long shopNumber = resolveShopNumber();
        LambdaQueryWrapper<GoodsDO> queryWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getShopNumber, shopNumber)
                .eq(GoodsDO::getDelFlag, 0)
                .like(StrUtil.isNotBlank(requestParam.getName()), GoodsDO::getName, requestParam.getName())
                .eq(ObjectUtil.isNotNull(requestParam.getCategoryId()), GoodsDO::getCategoryId, requestParam.getCategoryId())
                .eq(ObjectUtil.isNotNull(requestParam.getStatus()), GoodsDO::getStatus, requestParam.getStatus())
                .ge(ObjectUtil.isNotNull(requestParam.getMinPrice()), GoodsDO::getPrice, requestParam.getMinPrice())
                .le(ObjectUtil.isNotNull(requestParam.getMaxPrice()), GoodsDO::getPrice, requestParam.getMaxPrice())
                .orderByDesc(GoodsDO::getSortOrder)
                .orderByDesc(GoodsDO::getCreateTime);

        Page<GoodsDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<GoodsDO> selectPage = goodsMapper.selectPage(page, queryWrapper);

        return selectPage.convert(this::buildGoodsPageRespDTO);
    }

    @Override
    public GoodsQueryRespDTO findGoodsById(String goodsId) {
        checkGoodsPermission();

        String cacheKey = String.format(GOODS_CACHE_KEY, goodsId);
        Map<Object, Object> cached = stringRedisTemplate.opsForHash().entries(cacheKey);
        if (!cached.isEmpty()) {
            GoodsQueryRespDTO respDTO = new GoodsQueryRespDTO();
            respDTO.setId(parseLong(cached.get("id")));
            respDTO.setName(parseString(cached.get("name")));
            respDTO.setShopNumber(parseLong(cached.get("shopNumber")));
            respDTO.setCategoryId(parseLong(cached.get("categoryId")));
            respDTO.setDescription(parseString(cached.get("description")));
            respDTO.setMainImage(parseString(cached.get("mainImage")));
            respDTO.setPrice(parseBigDecimal(cached.get("price")));
            respDTO.setOriginalPrice(parseBigDecimal(cached.get("originalPrice")));
            respDTO.setStock(parseInteger(cached.get("stock")));
            respDTO.setSales(parseInteger(cached.get("sales")));
            respDTO.setUnit(parseString(cached.get("unit")));
            respDTO.setStatus(parseInteger(cached.get("status")));
            respDTO.setSortOrder(parseInteger(cached.get("sortOrder")));
            fillGoodsRelations(respDTO);
            return respDTO;
        }

        Long shopNumber = resolveShopNumber();
        GoodsDO goodsDO = getGoodsWithOwnerCheck(shopNumber, goodsId);
        GoodsQueryRespDTO respDTO = BeanUtil.toBean(goodsDO, GoodsQueryRespDTO.class);
        fillGoodsRelations(respDTO);
        return respDTO;
    }

    @Override
    public IPage<GoodsPageQueryRespDTO> pageQueryUserGoods(UserGoodsPageQueryReqDTO requestParam) {
        checkUserGoodsPermission();

        LambdaQueryWrapper<GoodsDO> queryWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getDelFlag, 0)
                .eq(GoodsDO::getStatus, GoodsStatusEnum.ON_SHELF.getStatus())
                .eq(ObjectUtil.isNotNull(requestParam.getShopNumber()), GoodsDO::getShopNumber, requestParam.getShopNumber())
                .like(StrUtil.isNotBlank(requestParam.getName()), GoodsDO::getName, requestParam.getName())
                .eq(ObjectUtil.isNotNull(requestParam.getCategoryId()), GoodsDO::getCategoryId, requestParam.getCategoryId())
                .ge(ObjectUtil.isNotNull(requestParam.getMinPrice()), GoodsDO::getPrice, requestParam.getMinPrice())
                .le(ObjectUtil.isNotNull(requestParam.getMaxPrice()), GoodsDO::getPrice, requestParam.getMaxPrice());
        applyUserGoodsSort(queryWrapper, requestParam.getSort());

        Page<GoodsDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<GoodsDO> selectPage = goodsMapper.selectPage(page, queryWrapper);
        return selectPage.convert(this::buildGoodsPageRespDTO);
    }

    @Override
    public GoodsQueryRespDTO findUserGoodsById(String goodsId, Long shopNumber) {
        checkUserGoodsPermission();
        if (StrUtil.isBlank(goodsId)) {
            throw new ClientException("商品ID不能为空");
        }
        if (shopNumber == null) {
            throw new ClientException("店铺编号不能为空");
        }

        LambdaQueryWrapper<GoodsDO> queryWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getId, goodsId)
                .eq(GoodsDO::getShopNumber, shopNumber)
                .eq(GoodsDO::getStatus, GoodsStatusEnum.ON_SHELF.getStatus())
                .eq(GoodsDO::getDelFlag, 0);
        GoodsDO goodsDO = goodsMapper.selectOne(queryWrapper);
        if (goodsDO == null) {
            throw new ClientException("商品不存在或已下架");
        }

        GoodsQueryRespDTO respDTO = BeanUtil.toBean(goodsDO, GoodsQueryRespDTO.class);
        fillGoodsRelations(respDTO, shopNumber);
        return respDTO;
    }

    @Override
    public void updateGoodsStatus(String goodsId, Integer status) {
        checkGoodsPermission();
        if (status != GoodsStatusEnum.OFF_SHELF.getStatus()
                && status != GoodsStatusEnum.ON_SHELF.getStatus()
                && status != GoodsStatusEnum.VIOLATION.getStatus()) {
            throw new ClientException("商品状态值不合法");
        }

        Long shopNumber = resolveShopNumber();
        GoodsDO goodsDO = getGoodsWithOwnerCheck(shopNumber, goodsId);

        GoodsDO updateDO = GoodsDO.builder().status(status).build();
        LambdaQueryWrapper<GoodsDO> updateWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getId, goodsId)
                .eq(GoodsDO::getShopNumber, shopNumber);
        goodsMapper.update(updateDO, updateWrapper);

        String cacheKey = String.format(GOODS_CACHE_KEY, goodsId);
        stringRedisTemplate.opsForHash().put(cacheKey, "status", String.valueOf(status));
    }

    @NoDuplicateSubmit(message = "请勿短时间内重复调整库存")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(GoodsStockReqDTO requestParam) {
        checkGoodsPermission();

        Long shopNumber = resolveShopNumber();
        Long goodsId = requestParam.getGoodsId();
        Integer quantity = requestParam.getQuantity();

        if (quantity == 0) {
            throw new ClientException("调整数量不能为0");
        }

        int result;
        if (quantity > 0) {
            result = goodsMapper.increaseStock(shopNumber, goodsId, quantity);
        } else {
            result = goodsMapper.decreaseStock(shopNumber, goodsId, Math.abs(quantity));
        }

        if (result == 0) {
            throw new ServiceException(quantity > 0 ? "增加库存失败" : "库存不足，减少库存失败");
        }

        String cacheKey = String.format(GOODS_CACHE_KEY, goodsId);
        stringRedisTemplate.opsForHash().increment(cacheKey, "stock", quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGoods(String goodsId) {
        checkGoodsPermission();
        Long shopNumber = resolveShopNumber();
        GoodsDO goodsDO = getGoodsWithOwnerCheck(shopNumber, goodsId);

        if (GoodsStatusEnum.ON_SHELF.getStatus() == goodsDO.getStatus()) {
            throw new ClientException("上架商品不能删除，请先下架");
        }

        GoodsDO updateDO = GoodsDO.builder().delFlag(1).build();
        LambdaQueryWrapper<GoodsDO> updateWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getId, goodsId)
                .eq(GoodsDO::getShopNumber, shopNumber);
        goodsMapper.update(updateDO, updateWrapper);

        deleteGoodsCache(goodsId);
    }

    @Override
    public List<GoodsQueryRespDTO> listGoodsByIds(List<String> goodsIds) {
        Long shopNumber = resolveShopNumber();
        LambdaQueryWrapper<GoodsDO> queryWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .in(GoodsDO::getId, goodsIds)
                .eq(GoodsDO::getShopNumber, shopNumber)
                .eq(GoodsDO::getDelFlag, 0);
        List<GoodsDO> goodsList = goodsMapper.selectList(queryWrapper);
        return goodsList.stream()
                .map(goods -> {
                    GoodsQueryRespDTO respDTO = BeanUtil.toBean(goods, GoodsQueryRespDTO.class);
                    fillGoodsRelations(respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
    }

    private GoodsPageQueryRespDTO buildGoodsPageRespDTO(GoodsDO goodsDO) {
        GoodsPageQueryRespDTO respDTO = BeanUtil.toBean(goodsDO, GoodsPageQueryRespDTO.class);
        GoodsCategoryDO category = goodsCategoryMapper.selectById(goodsDO.getCategoryId());
        if (category != null) {
            respDTO.setCategoryName(category.getName());
        }
        return respDTO;
    }

    private void applyUserGoodsSort(LambdaQueryWrapper<GoodsDO> queryWrapper, String sort) {
        if ("priceAsc".equals(sort)) {
            queryWrapper.orderByAsc(GoodsDO::getPrice).orderByDesc(GoodsDO::getCreateTime);
            return;
        }
        if ("priceDesc".equals(sort)) {
            queryWrapper.orderByDesc(GoodsDO::getPrice).orderByDesc(GoodsDO::getCreateTime);
            return;
        }
        if ("salesDesc".equals(sort)) {
            queryWrapper.orderByDesc(GoodsDO::getSales).orderByDesc(GoodsDO::getCreateTime);
            return;
        }
        if ("newest".equals(sort)) {
            queryWrapper.orderByDesc(GoodsDO::getCreateTime);
            return;
        }
        queryWrapper.orderByDesc(GoodsDO::getSortOrder).orderByDesc(GoodsDO::getCreateTime);
    }

    private void checkUserGoodsPermission() {
        if (UserRoleEnum.CUSTOMER.getType().equals(UserContext.getRoleType())) {
            return;
        }
        throw new ClientException("当前功能仅普通用户可访问");
    }

    private void checkGoodsPermission() {
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

    private GoodsDO getGoodsWithOwnerCheck(Long shopNumber, String goodsId) {
        LambdaQueryWrapper<GoodsDO> queryWrapper = Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getId, goodsId)
                .eq(GoodsDO::getShopNumber, shopNumber)
                .eq(GoodsDO::getDelFlag, 0);
        GoodsDO goodsDO = goodsMapper.selectOne(queryWrapper);
        if (goodsDO == null) {
            throw new ClientException("商品不存在或无权操作");
        }
        return goodsDO;
    }

    private void validateGoodsParam(GoodsSaveReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getName())) {
            throw new ClientException("商品名称不能为空");
        }
        if (requestParam.getName().length() > 256) {
            throw new ClientException("商品名称不能超过256个字符");
        }
        if (requestParam.getCategoryId() == null) {
            throw new ClientException("商品分类不能为空");
        }
        GoodsCategoryDO category = goodsCategoryMapper.selectById(requestParam.getCategoryId());
        if (category == null || category.getDelFlag() == 1) {
            throw new ClientException("商品分类不存在");
        }
        if (category.getStatus() != 0) {
            throw new ClientException("商品分类已禁用");
        }
        if (requestParam.getPrice() == null || requestParam.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ClientException("商品价格必须大于0");
        }
        if (requestParam.getOriginalPrice() != null && requestParam.getOriginalPrice().compareTo(requestParam.getPrice()) < 0) {
            throw new ClientException("原价不能低于售价");
        }
        if (requestParam.getStock() == null || requestParam.getStock() < 0) {
            throw new ClientException("库存数量不能为负数");
        }
        if (requestParam.getStock() > 999999) {
            throw new ClientException("库存数量不能超过999999");
        }
    }

    private void fillGoodsRelations(GoodsQueryRespDTO respDTO) {
        fillGoodsRelations(respDTO, null);
    }

    private void fillGoodsRelations(GoodsQueryRespDTO respDTO, Long shopNumber) {
        if (respDTO.getCategoryId() != null) {
            GoodsCategoryDO category = goodsCategoryMapper.selectById(respDTO.getCategoryId());
            if (category != null) {
                respDTO.setCategoryName(category.getName());
            }
        }

        LambdaQueryWrapper<GoodsImageDO> imageWrapper = Wrappers.lambdaQuery(GoodsImageDO.class)
                .eq(GoodsImageDO::getGoodsId, respDTO.getId())
                .eq(ObjectUtil.isNotNull(shopNumber), GoodsImageDO::getShopNumber, shopNumber)
                .orderByAsc(GoodsImageDO::getSortOrder);
        List<GoodsImageDO> images = goodsImageMapper.selectList(imageWrapper);
        respDTO.setImages(images.stream()
                .map(img -> {
                    GoodsQueryRespDTO.GoodsImageRespDTO imgDTO = new GoodsQueryRespDTO.GoodsImageRespDTO();
                    imgDTO.setId(img.getId());
                    imgDTO.setImageUrl(img.getImageUrl());
                    imgDTO.setSortOrder(img.getSortOrder());
                    return imgDTO;
                })
                .collect(Collectors.toList()));

        LambdaQueryWrapper<GoodsAttributeValueDO> attrWrapper = Wrappers.lambdaQuery(GoodsAttributeValueDO.class)
                .eq(GoodsAttributeValueDO::getGoodsId, respDTO.getId())
                .eq(ObjectUtil.isNotNull(shopNumber), GoodsAttributeValueDO::getShopNumber, shopNumber);
        List<GoodsAttributeValueDO> attrValues = goodsAttributeValueMapper.selectList(attrWrapper);
        respDTO.setAttributeValues(attrValues.stream()
                .map(attr -> {
                    GoodsQueryRespDTO.GoodsAttributeValueRespDTO attrDTO = new GoodsQueryRespDTO.GoodsAttributeValueRespDTO();
                    attrDTO.setAttributeId(attr.getAttributeId());
                    attrDTO.setAttributeValue(attr.getAttributeValue());
                    GoodsAttributeDO attributeDO = goodsAttributeMapper.selectById(attr.getAttributeId());
                    if (attributeDO != null) {
                        attrDTO.setAttributeName(attributeDO.getName());
                    }
                    return attrDTO;
                })
                .collect(Collectors.toList()));
    }

    private void cacheGoodsDetail(GoodsDO goodsDO) {
        if (goodsDO == null) {
            return;
        }
        String cacheKey = String.format(GOODS_CACHE_KEY, goodsDO.getId());
        Map<String, Object> cacheMap = BeanUtil.beanToMap(goodsDO, false, true);
        Map<String, String> stringCacheMap = cacheMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));
        stringRedisTemplate.opsForHash().putAll(cacheKey, stringCacheMap);
        stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private void deleteGoodsCache(String goodsId) {
        String cacheKey = String.format(GOODS_CACHE_KEY, goodsId);
        stringRedisTemplate.delete(cacheKey);
    }

    private Long parseLong(Object val) {
        if (val == null || val.toString().isEmpty()) return null;
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String parseString(Object val) {
        return val == null ? null : val.toString();
    }

    private Integer parseInteger(Object val) {
        if (val == null || val.toString().isEmpty()) return null;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseBigDecimal(Object val) {
        if (val == null || val.toString().isEmpty()) return null;
        try { return new BigDecimal(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
