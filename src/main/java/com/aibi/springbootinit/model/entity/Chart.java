package com.aibi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "chart")
@Data
public class Chart implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Long userId;
    private String goal;
    private String chartData;
    private String chartType;
    private String genChart;
    private String gemResult;
    private Date createTime;
    private Date updateTime;
    private String status;
    private String execMessage;
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
