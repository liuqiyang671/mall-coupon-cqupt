package com.mall.cqupt.merchant.admin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "商品创建参数")
public class GoodsSaveReqDTO {

    @Schema(description = "商品名称", example = "Apple iPhone 16 Pro", required = true)
    private String name;

    @Schema(description = "分类ID", example = "1", required = true)
    private Long categoryId;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "主图URL")
    private String mainImage;

    @Schema(description = "商品价格", example = "8999.00", required = true)
    private BigDecimal price;

    @Schema(description = "原价", example = "9999.00")
    private BigDecimal originalPrice;

    @Schema(description = "库存数量", example = "100", required = true)
    private Integer stock;

    @Schema(description = "计量单位", example = "件")
    private String unit;

    @Schema(description = "排序值", example = "0")
    private Integer sortOrder;

    @Schema(description = "商品图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "商品属性值列表")
    private List<GoodsAttributeValueReqDTO> attributeValues;

    @Data
    @Schema(description = "商品属性值")
    public static class GoodsAttributeValueReqDTO {

        @Schema(description = "属性ID", required = true)
        private Long attributeId;

        @Schema(description = "属性值", required = true)
        private String attributeValue;
    }
}
