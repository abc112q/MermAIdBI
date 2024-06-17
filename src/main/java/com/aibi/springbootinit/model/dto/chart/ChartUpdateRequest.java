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
 * 更新请求
 */
@Data
public class ChartUpdateRequest implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String goal;
    private String chartData;
    private String chartType;
    private String genChart;
    private String gemResult;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}