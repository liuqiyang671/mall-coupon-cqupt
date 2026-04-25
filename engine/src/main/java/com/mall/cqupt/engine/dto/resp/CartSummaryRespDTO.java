package com.mall.cqupt.engine.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "购物车汇总返回实体")
public class CartSummaryRespDTO {

    @Schema(description = "购物车项列表")
    private List<CartItemRespDTO> items;

    @Schema(description = "购物车总商品数")
    private Integer totalCount;

    @Schema(description = "选中商品数")
    private Integer selectedCount;

    @Schema(description = "商品总金额（未选+已选）")
    private BigDecimal totalAmount;

    @Schema(description = "已选商品总金额")
    private BigDecimal selectedAmount;

    @Schema(description = "已选商品节省金额（原价-售价）")
    private BigDecimal savedAmount;

    @Schema(description = "税费（已选商品的税费）")
    private BigDecimal taxAmount;

    @Schema(description = "应付金额（已选商品金额+税费）")
    private BigDecimal payableAmount;
}
