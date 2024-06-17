package com.aibi.springbootinit.model.dto.chart;

import lombok.Data;

@Data
public class genChartByAIRequest {
    private String name;
    private String goal;
    private String chartType;

    private static final long serialVersionUID = 1L;
}
