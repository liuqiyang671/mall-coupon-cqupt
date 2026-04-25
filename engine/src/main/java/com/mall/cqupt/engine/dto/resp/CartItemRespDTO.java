package com.mall.cqupt.engine.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "购物车项返回实体")
public class CartItemRespDTO {

    @Schema(description = "购物车项ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "店铺编号")
    private Long shopNumber;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品主图URL")
    private String mainImage;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "商品原价")
    private BigDecimal originalPrice;

    @Schema(description = "商品数量")
    private Integer quantity;

    @Schema(description = "小计金额")
    private BigDecimal subtotal;

    @Schema(description = "是否选中 0:未选中 1:选中")
    private Integer selected;

    @Schema(description = "商品状态 0:下架 1:上架 2:违规下架")
    private Integer goodsStatus;

    @Schema(description = "商品库存")
    private Integer goodsStock;

    @Schema(description = "加入购物车时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
