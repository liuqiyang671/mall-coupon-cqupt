package com.mall.cqupt.lqy.distribution.excel;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import com.mall.cqupt.lqy.distribution.common.constant.DistributionRedisConstant;
import com.mall.cqupt.lqy.distribution.mq.event.CouponTemplateExecuteEvent;
import com.mall.cqupt.lqy.distribution.mq.producer.CouponTemplateExecuteProducer;
import com.mall.cqupt.lqy.distribution.remote.dto.resp.CouponTemplateQueryRemoteRespDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 优惠券任务读取 Excel 分发监听器
 */
@RequiredArgsConstructor
public class ReadExcelDistributionListener extends AnalysisEventListener<CouponTaskExcelObject> {

    private final String couponTaskId;
    private final CouponTemplateQueryRemoteRespDTO couponTemplate;

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponTemplateExecuteProducer couponTemplateExecuteProducer;

    @Getter
    private int rowCount = 0;

    @Override
    public void invoke(CouponTaskExcelObject data, AnalysisContext context) {
        ++rowCount;

        // 获取当前进度，判断是否已经执行过。如果已执行，则跳过即可，防止执行到一半应用宕机
        String templateTaskExecuteProgressKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_PROGRESS_KEY, couponTaskId);
        String progress = stringRedisTemplate.opsForValue().get(templateTaskExecuteProgressKey);
        if (StrUtil.isNotBlank(progress) && Integer.parseInt(progress) > rowCount) {
            return;
        }

        // 为了避免数据库压力过大，这里通过消息队列进行削峰
        CouponTemplateExecuteEvent couponTemplateExecuteEvent = CouponTemplateExecuteEvent.builder()
                .userId(data.getUserId())
                .mail(data.getMail())
                .phone(data.getPhone())
                .couponTaskId(couponTaskId)
                .couponTemplate(couponTemplate)
                .build();
        couponTemplateExecuteProducer.sendMessage(couponTemplateExecuteEvent);

        // 同步当前执行进度到缓存
        stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // No additional actions needed after all data is analyzed
    }
}
