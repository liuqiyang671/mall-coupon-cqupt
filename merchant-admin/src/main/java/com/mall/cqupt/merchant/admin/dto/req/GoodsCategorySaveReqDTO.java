package com.mall.cqupt.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商品分类创建参数")
public class GoodsCategorySaveReqDTO {

    @Schema(description = "父分类ID，0表示一级分类", example = "0", required = true)
    private Long parentId;

    @Schema(description = "分类名称", example = "手机数码", required = true)
    private String name;

    @Schema(description = "分类图标URL")
    private String icon;

    @Schema(description = "排序值", example = "0")
    private Integer sortOrder;
}
