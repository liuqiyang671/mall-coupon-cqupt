package com.mall.cqupt.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(description = "商品分页查询返回实体")
public class GoodsPageQueryRespDTO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "店铺编号")
    private Long shopNumber;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "主图URL")
    private String mainImage;

    @Schema(description = "商品价格")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存数量")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "状态 0:下架 1:上架 2:违规下架")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
