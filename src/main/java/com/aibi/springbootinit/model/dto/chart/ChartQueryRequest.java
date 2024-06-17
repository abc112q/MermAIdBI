package com.aibi.springbootinit.model.dto.chart;


import com.aibi.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;


/**
 * 查询请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    private Long id;
    private String name;
    private String goal;
    private String chartType;
    private Long userId;

    private static final long serialVersionUID = 1L;
}