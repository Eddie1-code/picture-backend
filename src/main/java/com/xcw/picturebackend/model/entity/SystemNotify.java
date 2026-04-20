package com.xcw.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统通知
 *
 * @TableName t_system_notify
 */
@TableName(value = "t_system_notify")
@Data
public class SystemNotify implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String senderType;

    private String senderId;

    private String receiverType;

    private String receiverId;

    private String notifyType;

    private String notifyIcon;

    private String title;

    private String content;

    private String relatedBizType;

    private String relatedBizId;

    private Integer isGlobal;

    private Date expireTime;

    private Integer isEnabled;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
