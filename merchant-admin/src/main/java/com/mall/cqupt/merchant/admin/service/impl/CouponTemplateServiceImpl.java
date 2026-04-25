package com.mall.cqupt.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import com.mall.cqupt.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import com.mall.cqupt.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.mall.cqupt.merchant.admin.service.CouponTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.cqupt.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mall.cqupt.merchant.admin.common.constant.CouponTemplateConstant.*;
import static com.mall.cqupt.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;

@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private static final Long PLATFORM_TEMPLATE_SHOP_NUMBER = 0L;

    private final CouponTemplateMapper couponTemplateMapper;

    private final MerchantAdminChainContext merchantAdminChainContext;

    private final StringRedisTemplate stringRedisTemplate;

    @LogRecord(
            success = CREATE_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#bizNo}}",
            extra = "{{#requestParam.toString()}}"
    )
    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        checkCouponTemplatePermission();
        requestParam.setSource(resolveSourceForCurrentRole(requestParam.getSource()));

        // 通过责任链验证请求参数是否正确
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);

        // 新增优惠券模板信息到数据库
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        couponTemplateDO.setShopNumber(resolveTemplateShopNumber());
        couponTemplateMapper.insert(couponTemplateDO);

        // 因为模板 ID 是运行中生成的，@LogRecord 默认拿不到，所以我们需要手动设置
        LogRecordContext.putVariable("bizNo", couponTemplateDO.getId());

        // 缓存预热：通过将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
        // 使用 BeanUtil 工具类进行属性拷贝，将 couponTemplateDO 中的字段值复制到新创建的 CouponTemplateQueryRespDTO 对象中
        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);

        // 将 Java Bean 对象转换为 Map 结构，以便后续存入 Redis Hash
        // 第一个参数：待转换的 DTO 对象
        // 第二个参数 (isToUnderlineCase)：false，表示不将驼峰命名转换为下划线命名（保持字段名一致）
        // 第三参数 (ignoreNullValue)：true，表示如果 DTO 中某个字段为 null，则不放入 Map 中，避免覆盖 Redis 已有数据或存入无效数据
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);

        // 将 Map 的 Value 全部转换为 String 类型
        // 由于 Redis 的 stringRedisTemplate 要求 Hash 的 Key 和 Value 都必须是 String
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,             // 保持原来的 Key 不变
                        entry -> entry.getValue() != null ? entry.getValue().toString() : "" // 如果 Value 不为空则转为字符串，为空则给空字符串
                ));

        // 构建 Redis 中该优惠券模板对应的唯一 Key
        // 格式通常为 "one-coupon_engine:template:%s"，其中 %s 会被替换为具体的模板 ID
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateDO.getId());

        // 将处理好的 Map 一次性写入 Redis 的 Hash 结构中
        // opsForHash().putAll 对应 Redis 的 HMSET 命令，可以同时设置多个字段，性能比多次调用 put (HSET) 更高
        stringRedisTemplate.opsForHash().putAll(couponTemplateCacheKey, actualCacheTargetMap);
    }

    @Override
    public void updateCouponTemplate(String couponTemplateId, CouponTemplateSaveReqDTO requestParam) {
        checkCouponTemplatePermission();
        requestParam.setSource(resolveSourceForCurrentRole(requestParam.getSource()));

        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);

        Long ownerShopNumber = resolveTemplateShopNumber();
        Integer source = sourceForCurrentRole();
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, ownerShopNumber)
                .eq(CouponTemplateDO::getSource, source)
                .eq(CouponTemplateDO::getId, couponTemplateId);
        CouponTemplateDO originalCouponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        if (originalCouponTemplateDO == null) {
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }
        if (ObjectUtil.notEqual(originalCouponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已结束，不能继续编辑");
        }

        CouponTemplateDO updateCouponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        Wrapper<CouponTemplateDO> updateWrapper = Wrappers.lambdaUpdate(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getId, couponTemplateId)
                .eq(CouponTemplateDO::getShopNumber, ownerShopNumber)
                .eq(CouponTemplateDO::getSource, source);
        couponTemplateMapper.update(updateCouponTemplateDO, updateWrapper);

        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(updateCouponTemplateDO, CouponTemplateQueryRespDTO.class);
        actualRespDTO.setId(Long.parseLong(couponTemplateId));
        actualRespDTO.setStatus(originalCouponTemplateDO.getStatus());
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateId);
        stringRedisTemplate.opsForHash().putAll(couponTemplateCacheKey, actualCacheTargetMap);
    }

    @Override
    public IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        checkCouponTemplatePermission();

        // 构建分页查询模板 LambdaQueryWrapper
        Long ownerShopNumber = resolveTemplateShopNumber();
        Integer source = resolveSourceForCurrentRole(requestParam.getSource());
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, ownerShopNumber)
                .eq(CouponTemplateDO::getSource, source)
                .like(StrUtil.isNotBlank(requestParam.getName()), CouponTemplateDO::getName, requestParam.getName())
                .like(StrUtil.isNotBlank(requestParam.getGoods()), CouponTemplateDO::getGoods, requestParam.getGoods())
                .eq(Objects.nonNull(requestParam.getType()), CouponTemplateDO::getType, requestParam.getType())
                .eq(Objects.nonNull(requestParam.getTarget()), CouponTemplateDO::getTarget, requestParam.getTarget());

        // MyBatis-Plus 分页查询优惠券模板信息
        Page<CouponTemplateDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<CouponTemplateDO> selectPage = couponTemplateMapper.selectPage(page, queryWrapper);

        // 转换数据库持久层对象为优惠券模板返回参数
        return selectPage.convert(each -> BeanUtil.toBean(each, CouponTemplatePageQueryRespDTO.class));
    }

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId) {
        checkCouponTemplatePermission();

        Long ownerShopNumber = resolveTemplateShopNumber();
        Integer source = sourceForCurrentRole();
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, ownerShopNumber)
                .eq(CouponTemplateDO::getSource, source)
                .eq(CouponTemplateDO::getId, couponTemplateId);

        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        return BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
    }

    @LogRecord(
            success = TERMINATE_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#couponTemplateId}}"
    )
    @Override
    public void terminateCouponTemplate(String couponTemplateId) {
        checkCouponTemplatePermission();

        // 验证是否存在数据横向越权
        Long ownerShopNumber = resolveTemplateShopNumber();
        Integer source = sourceForCurrentRole();
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, ownerShopNumber)
                .eq(CouponTemplateDO::getSource, source)
                .eq(CouponTemplateDO::getId, couponTemplateId);
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        if (couponTemplateDO == null) {
            // 一旦查询优惠券不存在，基本可判定横向越权，可上报该异常行为，次数多了后执行封号等处理
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }

        // 验证优惠券模板是否正常
        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已结束");
        }

        // 记录优惠券模板修改前数据
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));

        // 修改优惠券模板为结束状态
        CouponTemplateDO updateCouponTemplateDO = CouponTemplateDO.builder()
                .status(CouponTemplateStatusEnum.ENDED.getStatus())
                .build();
        Wrapper<CouponTemplateDO> updateWrapper = Wrappers.lambdaUpdate(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getId, couponTemplateDO.getId())
                .eq(CouponTemplateDO::getShopNumber, ownerShopNumber)
                .eq(CouponTemplateDO::getSource, source);
        couponTemplateMapper.update(updateCouponTemplateDO, updateWrapper);

        // 修改优惠券模板缓存状态为结束状态
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateId);
        stringRedisTemplate.opsForHash().put(couponTemplateCacheKey, "status", String.valueOf(CouponTemplateStatusEnum.ENDED.getStatus()));
    }

    @LogRecord(
            success = INCREASE_NUMBER_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#requestParam.couponTemplateId}}"
    )
    @Override
    public void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam) {
        checkCouponTemplatePermission();

        // 验证是否存在数据横向越权
        Long ownerShopNumber = resolveTemplateShopNumber();
        Integer source = sourceForCurrentRole();
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, ownerShopNumber)
                .eq(CouponTemplateDO::getSource, source)
                .eq(CouponTemplateDO::getId, requestParam.getCouponTemplateId());
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        if (couponTemplateDO == null) {
            // 一旦查询优惠券不存在，基本可判定横向越权，可上报该异常行为，次数多了后执行封号等处理
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }

        // 验证优惠券模板是否正常
        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已结束");
        }

        // 记录优惠券模板修改前数据
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));

        // 设置数据库优惠券模板增加库存发行量
        int increased = couponTemplateMapper.increaseNumberCouponTemplate(ownerShopNumber, requestParam.getCouponTemplateId(), requestParam.getNumber());
        if (!SqlHelper.retBool(increased)) {
            throw new ServiceException("优惠券模板增加发行量失败");
        }

        // 增加优惠券模板缓存库存发行量
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        stringRedisTemplate.opsForHash().increment(couponTemplateCacheKey, "stock", requestParam.getNumber());
    }

    private void checkCouponTemplatePermission() {
        if (UserRoleEnum.PLATFORM.getType().equals(UserContext.getRoleType())) {
            return;
        }
        if (UserRoleEnum.MERCHANT.getType().equals(UserContext.getRoleType()) && UserContext.getShopNumber() != null) {
            return;
        }
        throw new ClientException("当前功能仅平台人员或商家角色可操作");
    }

    private Long resolveTemplateShopNumber() {
        if (UserRoleEnum.PLATFORM.getType().equals(UserContext.getRoleType())) {
            return PLATFORM_TEMPLATE_SHOP_NUMBER;
        }
        return UserContext.getShopNumber();
    }

    private Integer sourceForCurrentRole() {
        return UserRoleEnum.PLATFORM.getType().equals(UserContext.getRoleType()) ? 1 : 0;
    }

    private Integer resolveSourceForCurrentRole(Integer requestSource) {
        Integer expectedSource = sourceForCurrentRole();
        if (requestSource != null && !expectedSource.equals(requestSource)) {
            throw new ClientException(UserRoleEnum.PLATFORM.getType().equals(UserContext.getRoleType())
                    ? "平台人员只能管理平台券模板"
                    : "商家只能管理店铺券模板");
        }
        return expectedSource;
    }
}
