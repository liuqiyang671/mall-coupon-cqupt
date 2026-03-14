package com.mall.cqupt.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.mall.cqupt.engine.dao.entity.CouponTemplateDO;
import com.mall.cqupt.engine.dao.entity.CouponTemplateRemindDO;
import com.mall.cqupt.engine.dao.mapper.CouponTemplateRemindMapper;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.mall.cqupt.engine.mq.event.CouponRemindEvent;
import com.mall.cqupt.engine.mq.producer.CouponRemindProducer;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import com.mall.cqupt.engine.service.CouponTemplateService;
import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;
import com.mall.cqupt.engine.toolkit.CouponTemplateRemindUtil;
import com.mall.cqupt.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 优惠券预约提醒业务逻辑实现层

 */
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceRemindImpl extends ServiceImpl<CouponTemplateRemindMapper, CouponTemplateRemindDO> implements CouponTemplateRemindService {

    private final CouponTemplateService couponTemplateService;
    private final CouponTemplateRemindMapper couponTemplateRemindMapper;
    @Qualifier("cancelRemindBloomFilter")
    private final RBloomFilter<String> couponTemplateCancelRemindBloomFilter;
    private final CouponRemindProducer couponRemindProducer;

    @Override
    @Transactional
    public boolean createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam) {
        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, requestParam.getUserId())
                .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId());
        CouponTemplateRemindDO couponTemplateRemindDO = couponTemplateRemindMapper.selectOne(queryWrapper);
        if(couponTemplateRemindDO == null){
            // 如果没创建过提醒
            couponTemplateRemindDO = BeanUtil.toBean(requestParam, CouponTemplateRemindDO.class);
            couponTemplateRemindDO.setInformation(CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType()));
            couponTemplateRemindMapper.insert(couponTemplateRemindDO);
        } else {
            Long information = couponTemplateRemindDO.getInformation();
            Long bitMap = CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType());
            if ((information & bitMap) != 0L) {
                throw new ClientException("已经创建过该提醒了");
            }
            couponTemplateRemindDO.setInformation(information ^ bitMap);
            couponTemplateRemindMapper.update(couponTemplateRemindDO, queryWrapper);
        }
        couponRemindProducer.sendMessage(BeanUtil.toBean(requestParam, CouponRemindEvent.class));
        return true;
//        第一次来，点击预约【5分钟-APP推送】
//        前端传来的数据：userId = 小明, couponTemplateId = iPhone券, time = 5, type = 推送
//        查库： 数据库里一查，小明毫无记录，couponTemplateRemindDO == null。
//        走 if 分支：
//        调用公式计算 calculateBitMap(5, 推送)，假设算出专属钥匙是 2（二进制 0010）。
//        落库： 组装一条新数据，把 information 设为 2，执行 insert 插入数据库。
//        发MQ： 给 RocketMQ 发一条延迟消息：“距离抢券差5分钟的时候，给小明发个推送”。
//        结束： 此时数据库里有 1 行记录，information = 2。
    }

    @Override
    public List<CouponTemplateRemindQueryRespDTO> listCouponRemind(CouponTemplateRemindQueryReqDTO requestParam) {
        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, requestParam.getUserId());
        List<CouponTemplateRemindDO> couponTemplateRemindDOS = couponTemplateRemindMapper.selectList(queryWrapper);
        // 查出用户预约的信息
        if(couponTemplateRemindDOS == null || couponTemplateRemindDOS.isEmpty()){
            return new ArrayList<>();
        }
        // 根据优惠券id查询优惠券信息
        List<Long> couponIds = couponTemplateRemindDOS.stream().map(CouponTemplateRemindDO::getCouponTemplateId).toList();
        List<Long> shopNumbers = couponTemplateRemindDOS.stream().map(CouponTemplateRemindDO::getShopNumber).toList();
        List<CouponTemplateDO> couponTemplateDOS = couponTemplateService.listCouponTemplateById(couponIds, shopNumbers);
        List<CouponTemplateRemindQueryRespDTO> resp = BeanUtil.copyToList(couponTemplateDOS, CouponTemplateRemindQueryRespDTO.class);
        // 第一种实现：填充响应结果的其它信息
//        resp.forEach(each -> {
//            // 找到当前优惠券对应的预约提醒信息
//            couponTemplateRemindDOS.stream().filter(i -> i.getCouponTemplateId().equals(each.getId())).findFirst().ifPresent(i -> {
//                // 解析并填充预约提醒信息
//                CouponTemplateRemindUtil.fillRemindInformation(each, i.getInformation());
//            });
//        });
        // 第二种实现：
        // 1. 先将 List 转成 Map，Key 是优惠券 ID，Value 是提醒对象
        Map<Long, CouponTemplateRemindDO> remindMap = couponTemplateRemindDOS.stream()
                .collect(Collectors.toMap(CouponTemplateRemindDO::getCouponTemplateId, each -> each));
        // 2. 遍历结果集进行填充
        resp.forEach(each -> {
            // 直接从 Map 里取，时间复杂度从 O(m) 降到了 O(1)
            CouponTemplateRemindDO remindDO = remindMap.get(each.getId());
            if (remindDO != null) {
                CouponTemplateRemindUtil.fillRemindInformation(each, remindDO.getInformation());
            }
        });
        return resp;
    }

    @Override
    @Transactional
    public boolean cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam) {
        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, requestParam.getUserId())
                .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId());
        CouponTemplateRemindDO couponTemplateRemindDO = couponTemplateRemindMapper.selectOne(queryWrapper);
        // 计算bitMap信息
        Long bitMap = CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType());
        bitMap ^= couponTemplateRemindDO.getInformation();
        if (bitMap.equals(0L)) {
            // 如果新bitmap信息是0，说明已经没有预约提醒了，可以直接删除
            couponTemplateRemindMapper.delete(queryWrapper);
        } else {
            // 虽然删除了这个预约提醒，但还有其它提醒，更新数据库
            couponTemplateRemindDO.setInformation(bitMap);
            couponTemplateRemindMapper.updateById(couponTemplateRemindDO);
        }
        // 取消提醒这个信息添加到布隆过滤器中   感觉实际中不需要将取消优惠券消息加入布隆过滤器
        add2BloomFilter(requestParam.getCouponTemplateId(), requestParam.getUserId(), requestParam.getRemindTime(), requestParam.getType());
        return true;
    }

    private void add2BloomFilter(String couponTemplateId, String userId, Integer remindTime, Integer type) {
        couponTemplateCancelRemindBloomFilter.add(String.valueOf(Objects.hash(couponTemplateId, userId, remindTime, type)));
    }

    @Override
    public boolean isCancelRemind(RemindCouponTemplateDTO requestParam) {
        if (!couponTemplateCancelRemindBloomFilter.contains(String.valueOf(Objects.hash(requestParam.getCouponTemplateId(), requestParam.getUserId(), requestParam.getRemindTime(), requestParam.getType())))){
            // 布隆过滤器中不存在，说明没取消提醒，此时已经能挡下大部分请求
            return false;
        }
        // 对于少部分的“取消了预约”，可能是误判，此时需要去数据库中查找
        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, requestParam.getUserId())
                .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId());
        CouponTemplateRemindDO couponTemplateRemindDO = couponTemplateRemindMapper.selectOne(queryWrapper);
        if (null == couponTemplateRemindDO) {
            // 数据库中没该条预约提醒，说明被取消
            return true;
        }
        // 即使存在数据，也要检查该类型的该时间点是否有提醒
        Long information = couponTemplateRemindDO.getInformation();
        Long bitMap = CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType());
        // 按位与等于0说明用户取消了预约
        return (bitMap & information) == 0L;
    }
}
