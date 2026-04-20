package com.xcw.picturebackend.model.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 消息中心 - 通知子类型
 */
public enum NotifyTypeEnum {

    LIKE("like", "点赞"),
    COMMENT("comment", "评论"),
    FAVORITE("favorite", "收藏"),
    FOLLOW("follow", "关注"),
    SYSTEM("system", "系统通知");

    private final String value;
    private final String text;

    NotifyTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static NotifyTypeEnum from(String v) {
        if (StrUtil.isBlank(v)) return null;
        for (NotifyTypeEnum t : values()) {
            if (t.value.equalsIgnoreCase(v)) return t;
        }
        return null;
    }
}
