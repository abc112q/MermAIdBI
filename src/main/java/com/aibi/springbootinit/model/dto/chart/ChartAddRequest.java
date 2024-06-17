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
 * 添加图标都要什么请求
 *接收请求需要的属性要去看前端需要什么
 */
@Data
public class ChartAddRequest implements Serializable {
    private String goal;
    private String name; //表名
    private String chartData;
    private String chartType;

    private static final long serialVersionUID = 1L;
}