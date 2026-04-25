package com.mall.cqupt.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商品属性创建参数")
public class GoodsAttributeSaveReqDTO {

    @Schema(description = "属性名称", example = "颜色", required = true)
    private String name;

    @Schema(description = "输入类型 0:文本输入 1:单选 2:多选", example = "1", required = true)
    private Integer inputType;

    @Schema(description = "可选值列表，逗号分隔", example = "红色,蓝色,黑色")
    private String values;

    @Schema(description = "排序值", example = "0")
    private Integer sortOrder;
}
