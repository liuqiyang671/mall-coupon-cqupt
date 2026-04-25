package com.mall.cqupt.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_goods")
public class GoodsDO {

    private Long id;

    private Long shopNumber;

    private Long categoryId;

    private String name;

    private String description;

    private String mainImage;

    private java.math.BigDecimal price;

    private java.math.BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private String unit;

    private Integer status;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
