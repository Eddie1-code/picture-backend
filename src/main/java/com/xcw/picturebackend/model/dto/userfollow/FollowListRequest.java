package com.xcw.picturebackend.model.dto.userfollow;

import com.xcw.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询关注/粉丝列表 请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FollowListRequest extends PageRequest implements Serializable {

    /**
     * 要查询的用户 ID（不传则查当前登录用户）
     */
    private Long userId;

    /**
     * 列表类型：following-TA 的关注  fans-TA 的粉丝
     */
    private String type;

    private static final long serialVersionUID = 1L;
}
