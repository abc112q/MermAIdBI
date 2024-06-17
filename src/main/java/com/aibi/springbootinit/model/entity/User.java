package com.aibi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "user")
@Data
@NoArgsConstructor
public class User implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    //如果用Auto_id属于自动递增的有序序列
    private Long id;
    private String userAccount;
    private String userPassword;
    private String userName;
    private String userAvatar;
    private String userRole;
    private Date createTime;
    private Date updateTime;
    @TableLogic     //逻辑删除，并不会永久删除数据,看起来删除
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}