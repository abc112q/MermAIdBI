package com.aibi.springbootinit.model.vo;

import lombok.Data;

/**
 * 封装一个BI的返回结果
 */
@Data
public class BIResponse {

    private String genChart;
    private String genResult;
    private Long id;

    public void setChartID(Long id) {
        this.id=id;
    }
}
