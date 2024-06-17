package com.aibi.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 序列化一般用于网络传输或者对象持久化
 * 脱敏：对敏感数据进行处理，以保护用户隐私或遵守法规要求。
 * VO：viewobject
 */
@Data
public class LoginUserVO implements Serializable {
    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * ，实现了Serializable接口的类需要定义一个名为serialVersionUID的静态常量。
     * 这个常量的作用是为了在序列化和反序列化过程中确保类的版本一致性。
     */
    private static final long serialVersionUID = 1L;

}
