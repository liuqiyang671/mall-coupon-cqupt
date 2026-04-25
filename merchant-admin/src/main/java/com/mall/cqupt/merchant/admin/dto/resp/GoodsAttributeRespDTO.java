package com.mall.cqupt.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "商品属性返回实体")
public class GoodsAttributeRespDTO {

    @Schema(description = "属性ID")
    private Long id;

    @Schema(description = "属性名称")
    private String name;

    @Schema(description = "输入类型 0:文本输入 1:单选 2:多选")
    private Integer inputType;

    @Schema(description = "可选值列表，逗号分隔")
    private String values;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "状态 0:启用 1:禁用")
    private Integer status;
}
