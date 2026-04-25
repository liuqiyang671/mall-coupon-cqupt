package com.mall.cqupt.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "购物车选中状态更新请求参数")
public class CartSelectReqDTO {

    @Schema(description = "购物车项ID列表，为空则操作全部")
    private List<Long> cartIds;

    @Schema(description = "选中状态 0:取消选中 1:选中", required = true)
    private Integer selected;
}
