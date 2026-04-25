package com.mall.cqupt.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "商品分类返回实体")
public class GoodsCategoryRespDTO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图标URL")
    private String icon;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "分类层级")
    private Integer level;

    @Schema(description = "状态 0:启用 1:禁用")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "子分类列表")
    private List<GoodsCategoryRespDTO> children;
}
