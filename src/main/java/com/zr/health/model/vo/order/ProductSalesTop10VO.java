package com.zr.health.model.vo.order;

import lombok.Data;
import java.util.List;

/**
 * 商品销量Top10统计结果VO
 */
@Data
public class ProductSalesTop10VO {
    /**
     * 销量Top10商品列表
     */
    private List<ProductSalesVO> top10Products;

    /**
     * 统计时间范围说明（如：全部时间、近30天等）
     */
    private String timeRange;
}