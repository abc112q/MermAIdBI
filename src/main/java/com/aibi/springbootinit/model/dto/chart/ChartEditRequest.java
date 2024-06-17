package com.aibi.springbootinit.model.dto.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 编辑图表需要的请求
 */
@Data
public class ChartEditRequest implements Serializable {

    private Long id;          //对应编辑哪张图表
    private String name;
    private String goal;
    private String chartData;
    private String chartType;

    private static final long serialVersionUID = 1L;
}