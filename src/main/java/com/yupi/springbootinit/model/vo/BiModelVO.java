package com.yupi.springbootinit.model.vo;

import lombok.Data;

@Data
public class BiModelVO {

    /**
     * 生成的图表数据
     */
    private String getChart;

    /**
     * 生成的分析结果
     */
    private String getResult;

    /**
     * 图表id
     */
    private Long id;
}
